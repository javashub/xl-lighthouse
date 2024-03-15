import {
    Breadcrumb,
    Card, Checkbox, Form, Grid, Input, Notification, Space, Switch, Table, TableColumnProps, Typography,
} from '@arco-design/web-react';
import React, {useEffect, useState} from 'react';
import useLocale from '@/utils/useLocale';
import locale from "./locale";
import {IconCheck, IconClose, IconHome, IconPlus} from "@arco-design/web-react/icon";
import styles from "./style/index.module.less";
import {Stat} from "@/types/insights-web";
import {requestChangeState, requestQueryById} from "@/api/stat";
import {requestEnableDebugMode, requestDisableDebugMode, requestFetchTrackMessages} from "@/api/track";
import {useParams} from "react-router-dom";
import {getDataWithLocalCache} from "@/utils/localCache";
import ChartPanel from "@/pages/stat/preview/chart_panel";
import {DebugModeEnum} from "@/types/insights-common";
import {formatString, getRandomString} from "@/utils/util";
import get = Reflect.get;
import {formatTimeStamp, getDateFormat} from "@/utils/date";
import {getOrderTypeDescription} from "@/pages/common/desc/base";
const BreadcrumbItem = Breadcrumb.Item;

export default function TrackStatPage() {

    const t = useLocale(locale);
    const [formParams, setFormParams] = useState({});
    const [loading,setLoading] = useState<boolean>(false);
    const [listLoading,setListLoading] = useState<boolean>(false);
    const TextArea = Input.TextArea;
    const [monitorStatInfo,setMonitorStatInfo] = useState(null);
    const [statInfo,setStatInfo] = useState(null);
    const [groupId,setGroupId] = useState<number>(null);
    const [messagesColumns,setMessagesColumns] = useState([]);
    const [messageData,setMessageData] = useState([]);
    const [searchForm,setSearchForm] = useState(null);
    const [notifyMessages,setNotifyMessages] = useState([]);
    const [autoRefreshSwitch,setAutoRefreshSwitch] = useState<boolean>(false);
    const [intervalId,setIntervalId] = useState<any>(null);
    const {id} = useParams();
    const [formInstance] = Form.useForm();
    const { Text } = Typography;

    async function actualFetchMonitorStatInfo():Promise<Stat> {
        return new Promise<Stat>((resolve,reject) => {
            requestQueryById({id:1013}).then((response) => {
                resolve(response.data);
            }).catch((error) => {
                reject(error);
            })
        })
    }

    async function fetchMonitorStatInfo() {
        const monitorStatInfo = await getDataWithLocalCache('cache_cluster_monitor_1013',300,actualFetchMonitorStatInfo);
        setMonitorStatInfo(monitorStatInfo);
    }

    const fetchStatData = async () => {
        setLoading(true);
        await requestQueryById({id:id}).then((response) => {
            const {code, data ,message} = response;
            if(code == '0'){
                setStatInfo(data)
                setGroupId(data.groupId);
            }else{
                Notification.warning({style: { width: 420 }, title: 'Warning', content: message || t['system.error']});
            }
            setLoading(false);
        }).catch((error) => {
            console.log(error);
        })
    }

    const handlerRefreshChart = () => {
        setSearchForm({"date":[getDateFormat('YYYY-MM-DD'),getDateFormat('YYYY-MM-DD')],"captcha":["0","1","2"],"groupId":[groupId],t:Date.now()})
    }


    const fetchTrackMessages = async () => {
        await requestFetchTrackMessages({groupId:statInfo?.groupId,statId:statInfo?.id}).then((response) => {
            const {code, data ,message} = response;
            if(code == '0'){
                const keysArray = [];
                for(const key in data[0]){
                    if(data[0].hasOwnProperty(key)){
                        const col = {
                            title: key,
                            dataIndex: key,
                            key: key,
                            render: (value,record) => {
                                if(key == 'batchTime' || key == 'processTime' || key == 'messageTime'){
                                    return <Text>{formatTimeStamp(value,'YYYY-MM-DD hh:mm:ss')}</Text>
                                }else{
                                    return <Text>{value}</Text>
                                }
                            },
                        }
                        keysArray.push(col);
                    }
                }
                setMessagesColumns(keysArray);
                setMessageData(data);
            }else{
                Notification.warning({style: { width: 420 }, title: 'Warning', content: message || t['system.error']});
            }
        })
    }

    useEffect(() => {
        if(groupId){
            handlerRefreshChart();
            enableDebugMode(groupId).then();
            fetchTrackMessages().then();
        }
    },[groupId])

    useEffect(() => {
        fetchMonitorStatInfo().then()
        fetchStatData().then();
    },[id])

    const enableDebugMode = async (groupId:number) => {
        const changeParam = {
            id:groupId,
        }
        await requestEnableDebugMode(changeParam).then((response) => {
            const {code, data ,message} = response;
            if(code == '0'){
                const msg = formatString('调试模式已打开，调试周期：[%s ~ %s].',data.startTime,data.endTime);
                setNotifyMessages([msg])
                setAutoRefreshSwitch(true);
            }else if(code == '5001'){
                const msg = formatString('调试模式已处于开启状态，调试周期：[%s ~ %s].',formatTimeStamp(data.startTime,'YYYY-MM-DD hh:mm:ss'),formatTimeStamp(data.endTime,'YYYY-MM-DD hh:mm:ss'));
                setNotifyMessages([msg]);
                setAutoRefreshSwitch(true);
            }else{
                Notification.warning({style: { width: 420 }, title: 'Warning', content: message || t['system.error']});
            }
        }).catch((error)=>{
            console.log(error);
        })
    }

    function startInterval(){
        if(intervalId){
            clearInterval(intervalId);
        }
        const id = setInterval(() => {
            }, 30000);
        setIntervalId(id);
    }

    function stopInterval(){
        if(intervalId){
            clearInterval(intervalId);
        }
    }

    const changeCheckBox = (v) => {
        if(v){
            startInterval();
        }else{
            stopInterval();
        }
    }

    useEffect(() => {
        if(autoRefreshSwitch){
            startInterval();
        }else{
            stopInterval();
        }
        return () => {
            clearInterval(intervalId);
            console.log('定时任务已关闭');
        };
    },[autoRefreshSwitch])

    useEffect(() => {
        formInstance.setFieldValue("notifyArea",notifyMessages);
    },[notifyMessages])

    const disableDebugMode = async (groupId:number) => {
        const changeParam = {
            id:groupId,
        }
        await requestDisableDebugMode(changeParam).then((response) => {
            const {code, data ,message} = response;
            if(code == '0'){
                Notification.info({style: { width: 420 }, title: 'Notification', content: ""});
            }else{
                Notification.warning({style: { width: 420 }, title: 'Warning', content: message || t['system.error']});
            }
        }).catch((error)=>{
            console.log(error);
        })
    }

    return (
        <>
            <Breadcrumb style={{fontSize: 12,marginBottom:'10px'}}>
                <BreadcrumbItem>
                    <IconHome />
                </BreadcrumbItem>
                <BreadcrumbItem style={{fontWeight:20}}>{t['statTrack.breadcrumb.title']}</BreadcrumbItem>
            </Breadcrumb>
            <Space size={16} style={{width:'100%'}} direction="vertical">

                <div className={styles.wrapper}>
                    <Space size={16} direction="vertical" className={styles.left}>
                        <Card style={{height:'340px'}}>
                            {monitorStatInfo && searchForm && <ChartPanel parentLoading={false} statInfo={monitorStatInfo} size={'default'} searchForm={searchForm}/>}
                        </Card>
                    </Space>
                    <Space size={16} direction="vertical" className={styles.right}>
                        <Form form={formInstance} wrapperCol={{ span: 24 }}>
                            <Form.Item field={"notifyArea"}>
                                <TextArea
                                    placeholder='Please enter ...'
                                    readOnly={true}
                                    style={{ width: '100%',height:'340px',backgroundColor:'#373434',color:'white' }}
                                    defaultValue='This is the contents of the textarea. This is the contents of the textarea. This is the contents of the textarea. '
                                />
                            </Form.Item>
                        </Form>
                    </Space>
                </div>

                <Card style={{height:'50px'}}>
                    <Grid.Row gutter={8}>
                        <Grid.Col span={14}>
                            调试模式： <Switch checkedIcon={<IconCheck />} uncheckedIcon={<IconClose />} defaultChecked size={"small"} />
                            <span style={{marginLeft:'30px'}}>
                            当前统计组：behavior_stat，统计项：每分钟_uv数据统计
                            </span>
                        </Grid.Col>
                        <Grid.Col span={10} style={{textAlign:"right"}}>
                            <Grid.Row gutter={8}>
                                <Grid.Col span={18}>

                                </Grid.Col>
                                <Grid.Col span={6}>
                                    <Checkbox onChange={changeCheckBox}/>自动刷新
                                </Grid.Col>
                            </Grid.Row>
                        </Grid.Col>
                    </Grid.Row>
                </Card>
                <Card>
                    {messagesColumns && messageData && messageData.length > 0 && <Table size={"small"} pagination={false} rowKey={'Seq'} columns={messagesColumns} data={messageData} />}
                </Card>
            </Space>
        </>
    );
}