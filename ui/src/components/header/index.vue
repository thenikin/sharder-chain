<template>
    <header class="header">
        <div class="header_content">
            <div id="logo">
                <a href="#" class="logo">
                    <img src="../../assets/img/logo.svg"/>
                    <div>
                        <span>Sharder</span>
                        <span>{{blockchainStatus.application}}{{$t('header.version')}}{{blockchainStatus.fullVersion}}</span>
                    </div>
                </a>
            </div>
            <nav class="navbar_main" role="navigation">
                <el-menu class="navbar_left el-menu-demo" :class="this.$i18n.locale === 'en'? 'en_menu' : ''" mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="this.$route.path.indexOf('/account') >= 0 ? 'activeLi' : ''">{{$t('header.account')}}</el-menu-item>
                    <el-menu-item index="/network" :class="this.$route.path.indexOf('/network') >= 0 ? 'activeLi' : ''">{{$t('header.network')}}</el-menu-item>
                    <el-menu-item index="/mining" :class="this.$route.path.indexOf('/mining') >= 0 ? 'activeLi' : ''">{{$t('header.mining')}}</el-menu-item>
                </el-menu>
                <div class="navbar_console">
                    <el-button type="text" @click="goConsole">
                        <span class="console"></span>
                    </el-button>
                </div>
                <div class="navbar_search">
                    <div>
                        <input class="navbar_search_input" :class="activeSearch ? 'navbar_search_input_active' : ''"
                               :placeholder="placeholder" type="text" v-model="search_val"
                               @focus="search_focus" @blur="search_blur" @keyup.enter="search_keydown"/>
                        <img src="../../assets/img/search.svg" @click="search_keydown"/>
                    </div>
                </div>
                <div class="navbar_right">
                    <div class="navbar_status">
                        <span v-if="typeof(secretPhrase) === 'undefined'">{{accountRS}} | {{$t('header.observation_mode')}}</span>
                        <span class="isLogin" v-else>{{accountRS}} | {{$t('header.secret_mode')}}</span>
                    </div>
                    <div class="navbar_pilotLamp">
                        <el-tooltip class="item csp" :content="$t('account.please_init_hub')" placement="bottom" effect="light" v-if="isHubInit">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_new_account')" placement="bottom" effect="light" v-else-if="accountInfo.errorDescription === 'Unknown account'">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_effective_balance')" placement="bottom" effect="light" v-else-if="accountInfo.effectiveBalanceSS === 0">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_no_admin_password')" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) === 'undefined' && userConfig['sharder.HubBindAddress'] !== accountRS">
                            <div class="pilotLamp_circle unknownForging"  @click="startForging(false,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_exceeds_account_volume')" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) !== 'undefined' && userConfig['sharder.HubBindAddress']  !== accountRS">
                            <div class="pilotLamp_circle unknownForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.no_forging')" placement="bottom" effect="light" v-else-if="forging.errorCode === 5">
                            <div class="pilotLamp_circle notForging"  @click="startForging(true,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item" :content="$t('header.started_forging')" placement="bottom" effect="light" v-else-if="!forging.errorDescription">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>

                    </div>
                    <div class="navbar_exit">
                        <span class="csp" @click="exit"><a>{{$t('header.exit')}}</a></span>
                    </div>
                    <div class="navbar_lang">
                        <el-select v-model="selectLan">
                            <el-option
                                v-for="item in language"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                </div>
            </nav>
        </div>
        <dialogCommon :searchValue="search_val" :isSearch="isSearch" @isClose="isClose"></dialogCommon>

        <div class="modal" id="start_forging_modal" v-show="startForgingDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t('header.start_forging')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('header.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="startForging(false,adminPassword)">{{$t('header.starting_forging')}}</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="download_blocks_loading" v-if="isDownLoadingBlockchain">
            <div class="download_blocks_loading_active" v-show="isDownloadingState === 'isActive'">
                <div>
                    <span>{{$t("account.downloading_blockchain")}}</span>
                    <span v-if="blocksLeft">（{{blocksLeft}}{{$t("account.remaining_blocks")}}）</span>
                </div>
                <div class="download_blocks_progress_total" v-if="blocksLeft && blocksLeft >= 2500">
                    <el-progress color="rgba(73, 62, 218)" :text-inside="true" :stroke-width="18" :percentage="percentageTotal"></el-progress>
                </div>
                <div class="download_blocks_progress_last" v-if="blocksLeft && blocksLeft < 10000 && lastBlockchainFeederHeight > 5000">
                    <el-progress color="rgba(73, 62, 218)" :text-inside="true" :stroke-width="18" :percentage="percentageLast"></el-progress>
                </div>
            </div>
            <div  v-show="isDownloadingState === 'isLightClient'">
                <p>Light Client</p>
                <p>{{$t("account.block_fully_downloaded")}}</p>
            </div>
            <div  v-show="isDownloadingState === 'isHalted'">
                <span>{{$t("account.download_interrupt")}}</span>
            </div>
        </div>
    </header>

</template>

<script>
    export default {
        name: "Header",
        props: ["openSidebar", "title"],
        data () {
            return {
                startForgingDialog:false,
                activeIndex: "/account",
                isRouter: true,
                placeholder: this.$t('header.search'),
                activeSearch: false,
                blockchainStatus:"",
                secretPhrase:SSO.secretPhrase,
                adminPassword:'',
                accountRS:SSO.accountRS,
                accountInfo:[],
                forging:[],
                userConfig:[],
                search_val: "",
                isSearch:false,
                isHubInit:this.$store.state.isHubInit,
                selectLan:'',
                selectLanValue:'',
                language:[{
                    value:'cn',
                    label:'简体中文'
                },{
                    value:'en',
                    label:'English'
                }],

                isDownLoadingBlockchain:SSO.downloadingBlockchain,
                isDownloadingState:SSO.isDownloadingState,
                isProgressTotalShow:SSO.isProgressTotalShow,
                isProgressLastShow:SSO.isProgressLastShow,
                isBlockOutLeft:SSO.isBlockOutLeft,
                lastBlockchainFeederHeight:SSO.state.lastBlockchainFeederHeight,
                percentageTotal:SSO.percentageTotal,
                blocksLeft:SSO.blocksLeft,
                percentageLast:SSO.percentageLast,
            };
        },
        created(){
            const _this = this;

            let lang = this.$i18n.locale;
            if(typeof lang !== 'undefined'){

                for(let i=0;i<_this.language.length;i++){
                    if(_this.language[i].value === lang){
                        _this.selectLan = _this.language[i].label;
                        _this.selectLanValue = _this.language[i].value;
                    }
                }
            }else{
                _this.selectLan = _this.language[value === 'cn'].label;
                _this.selectLanValue = _this.language[value === 'cn'].value;
            }



            this.getData();
            this.$http.get("/sharder?requestType=getAccount",{
                params: {
                    includeEffectiveBalance:true,
                    account:SSO.account
                }
            }).then(res=>{
                _this.accountInfo = res.data;
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });
            _this.$global.getUserConfig(_this).then(res=>{
                _this.userConfig = res;
            });

            let formData = new FormData();
            formData.append("secretPhrase",_this.secretPhrase);
            let config = {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            };
            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                _this.forging = res.data;
                // console.log("forging",_this.forging);
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });
        },
        mounted(){
            let _this = this;
            setInterval(()=>{
                _this.getData();
            },30000);
            setInterval(()=>{
                _this.isDownLoadingBlockchain = SSO.downloadingBlockchain;
                _this.isDownloadingState = SSO.isDownloadingState,
                    _this.isProgressTotalShow = SSO.isProgressTotalShow;
                _this.isProgressLastShow = SSO.isProgressLastShow;
                _this.isBlockOutLeft = SSO.isBlockOutLeft;
                _this.lastBlockchainFeederHeight = SSO.state.lastBlockchainFeederHeight;
                _this.percentageTotal = SSO.percentageTotal;
                _this.blocksLeft = SSO.blocksLeft;
                _this.percentageLast = SSO.percentageLast;
            },2000);
        },
        methods: {

            getData:function(){
                const _this = this;
                // if(_this.i%30 === 0){
                    _this.$global.setBlockchainState(_this).then(res=>{
                        _this.blockchainStatus = res.data;
                        /*if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                        }*/
                        SSO.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res.data,res);
                    });
                    _this.$global.setUnconfirmedTransactions(_this,SSO.account).then(res=>{
                        _this.$store.state.unconfirmedTransactionsList = res.data;
                        console.log("unconfirmedTransactionsList",res.data);
                        /*if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getUnconfirmedTransactions",'GET',res);
                        }*/
                        SSO.addToConsole("/sharder?requestType=getUnconfirmedTransactions",'GET',res.data,res);
                    });
                    _this.$global.setPeers(_this).then(res=>{
                        /*if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getPeers",'GET',res);
                        }*/
                        SSO.addToConsole("/sharder?requestType=getPeers",'GET',res.data,res);
                    });
                // }
            },
            startForging:function(b,pwd){
                const _this = this;
                let formData = new FormData();
                let config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                };
                if(b){
                    formData.append("secretPhrase",SSO.secretPhrase);
                    _this.$http.post("/sharder?requestType=startForging",formData,config).then(res=>{
                        if(!res.data.errorDescription){
                            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err=>{
                                _this.$message.error(err);
                                console.error(err);
                            });

                        }else{
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err=>{
                        _this.$message.error(err);
                        console.error(err);
                    });
                }else if(b === false&&pwd === ''){
                    _this.startForgingDialog = true;
                    _this.$store.state.mask = true;
                }else{
                    formData.append("secretPhrase",pwd);
                    _this.$http.post("/sharder?requestType=startForging",formData,config).then(res=>{
                        if(!res.data.errorDescription){
                            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err=>{
                                _this.$message.error(err);
                                console.error(err);
                            });
                        }else{
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err=>{
                        _this.$message.error(err);
                        console.error(err);
                    });
                    closeDialog();
                }
            },
            activeItem: function (val) {
                const _this = this;
                _this.activeIndex = val;
            },
            goConsole: function () {
                // const _this = this;

                SSO.showConsole(this);

               /* _this.$global.newConsole = window.open("", "console", "width=750,height=400,menubar=no,scrollbars=yes,status=no,toolbar=no,resizable=yes");
                $(_this.$global.newConsole.document.head).html("<title>CONSOLE</title><style type='text/css'>body { background:black; color:white; font-family:courier-new,courier;font-size:14px; } pre { font-size:14px; } #console { padding-top:15px; }</style>");
                $(_this.$global.newConsole.document.body).html("<div style='position:fixed;top:0;left:0;right:0;padding:5px;background:#efefef;color:black;'>"+_this.$t('header.open_console')+"<div style='float:right;text-decoration:underline;color:blue;font-weight:bold;cursor:pointer;' onclick='document.getElementById(\"console\").innerHTML=\"\"'>clear</div></div><div id='console'></div>");
               */
               /* let loop = setInterval(function() {
                    if(_this.$global.newConsole.closed) {
                        clearInterval(loop);
                        _this.$global.isOpenConsole = false;
                    }
                }, 1000);*/
                // this.$global.isOpenConsole = true;
            },
            search_focus: function () {
                const _this = this;
                _this.activeSearch = true;
                _this.placeholder = _this.$t('header.search_open');
            },
            search_blur: function () {
                const _this = this;
                if (_this.search_val === "") {
                    _this.activeSearch = false;
                    _this.placeholder = _this.$t('header.search');
                }
            },
            search_keydown: function () {
                const _this = this;
                if(_this.search_val !== ""){
                    _this.isSearch = true;

                }else{
                    _this.$message({
                        showClose: true,
                        message: _this.$t('notification.search_no_null_error'),
                        type: "error"
                    });
                }
            },
            closeDialog:function(){
                this.startForgingDialog = false;
                this.$store.state.mask = false;
            },
            exit:function () {
                window.location.href = "/";
            },
            isClose:function () {
                const _this = this;
                _this.isSearch = false;

            },
        },
        watch:{
            selectLan:function (language) {
                const _this = this;
                for(let i=0;i<_this.language.length;i++){
                    if(_this.language[i].value === language){
                        _this.$i18n.locale = language;
                        _this.$store.commit('updateLang',language);
                        _this.selectLanValue = language;
                    }
                }}
        },
    };
</script>
<style lang="scss" type="text/scss">
    /* You can import all your SCSS variables using webpack alias*/
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped  lang="scss" type="text/scss">
    .el-select-dropdown{
        .el-select-dropdown__item.selected{
            background-color: #493eda!important;
            color: #fff!important;
        }
        .el-select-dropdown__item.selected.hover{
            background-color: #493eda!important;
            color: #fff!important;
        }
    }
    .en_menu{
        .el-menu-item {
            font-size: 12px!important;
        }
    }
    .download_blocks_loading{
        display: block;
        width: 330px;
        height: 110px;
        padding: 14px 26px 14px 13px;
        border-radius: 8px;
        box-sizing: border-box;
        border: 1px solid #ebeef5;
        position: fixed;
        background-color: #fff;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        transition: opacity .3s,transform .3s,left .3s,right .3s,top .4s,bottom .3s;
        overflow: hidden;
        right: 20px;
        top: 100px;
        z-index: 2;
        p{
            margin-bottom: 10px;
        }
    }
</style>
