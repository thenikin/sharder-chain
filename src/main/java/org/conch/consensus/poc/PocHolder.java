package org.conch.consensus.poc;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.Generator;
import org.conch.peer.Peer;
import org.conch.util.IpUtil;
import org.conch.util.Logger;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocHolder is a singleton to hold the score map.
 * This map stored in the memory, changed by the poc txs.
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */

public class PocHolder implements Serializable {

    static PocHolder inst = new PocHolder();
    
    
    // accountId : pocScore
    Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
    // height : { accountId : pocScore }
    Map<Integer,Map<Long,PocScore>> historyScore = new ConcurrentHashMap<>();

    // certified miner: foundation node,sharder hub, community node
    // height : <bindAccountId,peer>
    Map<Integer, Map<Long, Peer>> certifiedMinerPeerMap = new ConcurrentHashMap<>();
    // peerType : <bindAccountId,peerIp> 
    Map<Peer.Type, Map<Long, String>> certifiedBindAccountMap = Maps.newConcurrentMap();

    int lastHeight = -1;

    static {
        initBindMiners();
    }

    private static void initBindMiners(){
        inst.certifiedBindAccountMap.put(Peer.Type.HUB,Maps.newConcurrentMap());
        inst.certifiedBindAccountMap.put(Peer.Type.COMMUNITY,Maps.newConcurrentMap());
        inst.certifiedBindAccountMap.put(Peer.Type.FOUNDATION,Maps.newConcurrentMap());


        String ip = Constants.isDevnet() ? IpUtil.getIp("devboot.sharder.io") : Constants.isTestnet() ? IpUtil.getIp("testboot.sharder.io") : IpUtil.getIp("mainboot.sharder.io");
        inst.certifiedBindAccountMap.get(Peer.Type.FOUNDATION).put(SharderGenesis.CREATOR_ID,ip);

        if(Constants.isDevnet()){
            List<GenesisRecipient> recipients = GenesisRecipient.getAll();
            recipients.forEach(recipient -> inst.certifiedBindAccountMap.get(Peer.Type.FOUNDATION).put(recipient.id,ip));
        }else if(Constants.isTestnet()){
            //TODO default testnet accounts 
        }else if(Constants.isMainnet()){
            //TODO default mainnet accounts 
        }
    }

    private PocHolder(){}


    private static void _defaultPocScore(long accountId,int height){
        scoreMapping(new PocScore(accountId,height));
    }

    /**
     * get the poc score of the specified height
     * @param height
     * @param accountId
     * @return
     */
    static BigInteger getPocScore(int height, long accountId) {
        if(height < 0) height = 0;
        if (!inst.scoreMap.containsKey(accountId)) {
            PocProcessorImpl.notifySynTxNow();
            _defaultPocScore(accountId,height);
        }

        PocScore pocScore = inst.scoreMap.get(accountId);
        //newest poc score when query height is bigger than last height of poc score 
        if(pocScore.height <= height) {
            return pocScore.total();
        }else{
            //get from history
            pocScore = getHistoryPocScore(height, accountId);
            if(pocScore != null) {
                return pocScore.total();
            }
        }
        return BigInteger.ZERO;
    }

    /**
     * get the detailed poc score of the specified height
     *
     * @param height
     * @param accountId
     * @return json string
     */
    static JSONObject getDetailedPocScore(int height, long accountId) {
        if(height < 0) height = 0;
        PocScore pocScore;
        if (!inst.scoreMap.containsKey(accountId)) {
            PocProcessorImpl.notifySynTxNow();
            _defaultPocScore(accountId,height);
        }
        pocScore = inst.scoreMap.get(accountId);
        //newest poc score when query height is bigger than last height of poc score
        if(pocScore.height > height) {
            //get from history
            pocScore = getHistoryPocScore(height, accountId);
        }
        if (pocScore != null) {
            return pocScore.toJsonObject();
        }
        return null;
    }
    
    /**
     * update the poc score of account
     * @param pocScore a poc score object
     */
    static synchronized void scoreMapping(PocScore pocScore){
        PocScore _pocScore = pocScore;
        if(inst.scoreMap.containsKey(pocScore.accountId)) {
            _pocScore = inst.scoreMap.get(pocScore.accountId);
            _pocScore.synScoreFrom(pocScore);
            recordHistoryScore(pocScore);
        }

        inst.scoreMap.put(pocScore.accountId,_pocScore);
        inst.lastHeight = pocScore.height > inst.lastHeight ? pocScore.height : inst.lastHeight;
        PocScorePrinter.print();
    }

    static BigInteger getTotal(int height,Long accountId){
        Map<Long,PocScore> map = inst.historyScore.get(height);
        if(map == null) return BigInteger.ZERO;
        PocScore score = map.get(accountId);
        return score !=null ? score.total() : BigInteger.ZERO;
    }
        
    /**
     * record current poc score into history
     */
    static void recordHistoryScore(PocScore pocScore){
        Map<Long,PocScore> map = inst.historyScore.get(pocScore.height);
        if(map == null) map = new HashMap<>();

        map.put(pocScore.accountId,new PocScore(pocScore.height, pocScore));

        inst.historyScore.put(pocScore.height,map);
    }

    static PocScore getHistoryPocScore(int height,long accountId){
        if(!inst.historyScore.containsKey(height)) {
            return null;
        }
        PocScorePrinter.print();
        return inst.historyScore.get(height).get(accountId);
    }

    static PocTxBody.PocWeightTable getPocWeightTable(){
        return PocCalculator.inst.getCurWeightTable();
    }

    /**
     * Poc score map printer
     */
    private static class PocScorePrinter {
        static int count = 0;
        static final int printCount = 100;
        
        protected static boolean debug = Constants.isDevnet() ? true : false;
        protected static String summary = reset();

        private static final String splitter = "\n\r";

        static private String appendSplitter(String str, boolean appendEnd) {
            str = splitter + str ;
            if(appendEnd) {
                str += splitter;
            }
            return str;
        }
        
        static String reset(){
            String summary = appendSplitter("--------------PocScorePrinter-------------",false);
            count=0;
            return summary;
        }
        
        static String scoreMapStr( Map<Long, PocScore> scoreMap){
            Set<Long> accountIds = scoreMap.keySet();
            for(Long accountId : accountIds){
                PocScore pocScore = scoreMap.get(accountId);
                summary += appendSplitter(Account.rsAccount(pocScore.accountId) + ",poc score=" + pocScore.total() + ":" + pocScore.toJsonString(),false);
            }
            return summary;
        }
        
        static void putin(){
            // accountId : pocScore
            Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
            // height : { accountId : pocScore }
            Map<Integer,Map<Long,PocScore>> historyScore = new ConcurrentHashMap<>();

            summary += appendSplitter("PocScore & Height Map[ accountId : PocScore Object ] >>>>>>>>",true);
            scoreMapStr(inst.scoreMap);
            summary += appendSplitter("<<<<<<<<<<",true);

            summary += appendSplitter("PocScore & Height Map[ height : Map{ accountId : PocScore Object } ] >>>>>>>>",true);
            Set<Integer> heights = inst.historyScore.keySet();
            
            for(Integer height : heights){
                summary += "height -> {";
                scoreMapStr(inst.historyScore.get(height));
                summary += appendSplitter("}",true);
            }
            summary += appendSplitter("<<<<<<<<<<",true);
        }

        static void print(){
            if(!debug || (count++  <= printCount)) return;
            putin();
            Logger.logDebugMessage(summary);
            summary = reset();
        }
    }

}