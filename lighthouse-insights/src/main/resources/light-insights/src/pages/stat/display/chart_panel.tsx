import React, {useEffect, useRef, useState} from 'react';
import {useParams} from "react-router-dom";
import {Stat, StatData, EChartChartValue} from "@/types/insights-web";
import {Notification, Space} from "@arco-design/web-react";
import {requestData, requestTestData} from "@/api/data";
import useLocale from "@/utils/useLocale";
import locale from "./locale";
import ReactECharts from 'echarts-for-react';
import echarts from "echarts";
import {
    convertDateToTimestamp,
    getDailyEndTimestamp,
    getDailyStartTimestamp,
 DateFormat, getDayBefore, getDayStartTimestamp, getDayEndTimestamp
} from "@/utils/date";

export default function ChartPanel({size = 'default',searchForm={},statInfo}:{size:string,searchForm:any,statInfo:Stat}) {
    const t = useLocale(locale);
    const [loading,setLoading] = useState<boolean>(false);
    const [batchTimeList,setBatchTimeList] = useState<string[]>([]);
    const [eChartData,setEChartData] = useState<Array<EChartChartValue>>([]);

    const loadData = (data:Array<StatData>) => {
        const eChartChartValues:Array<EChartChartValue> = [];
        if(data && data.length > 0){
            setBatchTimeList(data[0].valuesList?.map(z => z.displayBatchTime))
        }
        data?.forEach(z => {
            const dimens = z.dimens;
            const dimensValue = z.dimensValue;
            const displayDimensValue = z.displayDimensValue;
            const statId = z.statId;
            const seriesObject:EChartChartValue = {
                name:dimensValue,
                type:'line',
                data:z.valuesList?.map(z => z.value),
            }
            eChartChartValues.push(seriesObject);
        })
        setEChartData(eChartChartValues);
    }


    const fetchData = async () => {
        setLoading(true);
        const combineParam:any = {}
        combineParam.statId = statInfo?.id;
        if(searchForm != null){
            const date = searchForm.date;
            if(date && Array.isArray(date)){
                combineParam.startTime = getDayStartTimestamp(convertDateToTimestamp(date[0],DateFormat));
                combineParam.endTime = getDayEndTimestamp(convertDateToTimestamp(date[1],DateFormat));
            }
        }else{
            const timeParam = statInfo?.timeparam;
            if(timeParam.endsWith('minute') || timeParam.endsWith('second') || timeParam.endsWith('hour')){
                combineParam.startTime = getDailyStartTimestamp();
                combineParam.endTime = getDailyEndTimestamp();
            }else if(timeParam.endsWith('day')){
                combineParam.startTime = getDayBefore(getDailyStartTimestamp(),14);
                combineParam.endTime = getDailyEndTimestamp();
            }else if(timeParam.endsWith('month')){
                combineParam.startTime = getDayBefore(getDailyStartTimestamp(),365);
                combineParam.endTime = getDailyEndTimestamp();
            }
        }
        await requestTestData(combineParam).then((response) => {
            const {code, data ,message} = response;
            if(code == '0'){
                loadData(data);
            }else{
                Notification.warning({style: { width: 420 }, title: 'Warning', content: message || t['system.error']});
            }
            setLoading(false);
        }).catch((error) => {
            console.log(error);
        })
    }

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'line',
                label: {
                    backgroundColor: '#6a7985'
                }
            }
        },
        dataZoom: [
            {
                type: 'inside',
                start: 0,
                end: 100
            }
        ],
        legend: {
            data: eChartData.map(z => z.name)
        },
        toolbox: {
            feature: {
                saveAsImage: {}
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: [
            {
                type: 'category',
                boundaryGap: false,
                data: batchTimeList
            }
        ],
        yAxis: [
            {
                type: 'value'
            }
        ],
        series: eChartData
    };

    useEffect(() => {
        fetchData().then();
    },[JSON.stringify(searchForm)])

    const getReactChart = () => {
        if(size == 'default'){
            return <ReactECharts option={option} style={{ height: '350px' ,width:'100%',marginLeft:'0px'}} />
        }else if(size == 'small'){
            return <ReactECharts option={option} style={{ height: '230px' ,width:'100%',marginLeft:'0px'}} />
        }else if(size == 'mini'){
            return <ReactECharts option={option} style={{ height: '150px' ,width:'100%',marginLeft:'0px'}} />
        }
    }

    return (<>
        <Space size={16} direction="vertical" style={{ width: '100%' }}>
            {getReactChart()}
        </Space>
    </>);
}