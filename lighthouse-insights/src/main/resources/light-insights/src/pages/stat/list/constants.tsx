import {
    Badge,
    Button,
    Popconfirm,
    Popover,
    Space,
    TableColumnProps,
    Tabs,
    Tooltip,
    Typography
} from "@arco-design/web-react";
import React from "react";
import {IconStar, IconStarFill} from "@arco-design/web-react/icon";
import {PiLinkSimple} from "react-icons/pi";
import {StatExpiredEnum, StatStateEnum} from "@/types/insights-common";
const TabPane = Tabs.TabPane;
const { Text } = Typography;
export function getColumnsOfManage(t: any, callback: (record: Record<string, any>, type: string) => Promise<void>) {

    return [
        {
            title: '',
            dataIndex: 'binded',
            headerCellStyle: { width:'20px' },
            render: (_, record) => {
                return <Button icon={<PiLinkSimple/>} size={"mini"} shape={"round"} onClick={() => {callback(record, 'binded')}}/>
            }
        },
        {
            title: 'ID',
            dataIndex: 'id',
        },
        {
            title: 'Title',
            dataIndex: 'title',
            render:(value,record) => {
                return (<div onClick={() => callback(record, 'showDetailModal')} style={{ cursor: "pointer" }} ><Text>{value}</Text></div>)
            }
        },
        {
            title: 'Group',
            dataIndex: 'group.token',
        },
        {
            title: 'TimeParam',
            dataIndex: 'timeparam',
        },
        {
            title: 'Expired',
            dataIndex: 'expired',
            render:(value,record) => {
                if(value == StatExpiredEnum.Week_1){
                    return '1 Week';
                }else if(value == StatExpiredEnum.Week_2){
                    return '2 Week';
                }else if(value == StatExpiredEnum.MONTH_1){
                    return '1 month';
                }else if(value == StatExpiredEnum.MONTH_2){
                    return '2 month';
                }else if(value == StatExpiredEnum.MONTH_3){
                    return '3 month';
                }else if(value == StatExpiredEnum.MONTH_6){
                    return '6 month';
                }else if(value == StatExpiredEnum.MONTH_12){
                    return '12 month';
                }else if(value == StatExpiredEnum.MONTH_24){
                    return '24 month';
                }else if(value == StatExpiredEnum.MONTH_36){
                    return '36 month';
                }
            }
        },
        {
            title: 'State',
            dataIndex: 'state',
            render: (value) => {
                if(value === StatStateEnum.PENDING){
                    return <Badge color={'gold'} text={t['statList.columns.state.pending']}/>;
                }else if (value === StatStateEnum.RUNNING) {
                    return <Badge color={'arcoblue'} text={t['statList.columns.state.running']}/>;
                }else if(value === StatStateEnum.STOPPED){
                    return <Badge color={'gray'} text={t['statList.columns.state.limiting']}/>;
                }else if(value === StatStateEnum.LIMITING){
                    return <Badge color={'pinkpurple'} text={t['statList.columns.state.frozen']}/>;
                }else if(value === StatStateEnum.DELETED){
                    return <Badge color={'cyan'} text={t['statList.columns.state.frozen']}/>;
                }else if(value === StatStateEnum.REJECTED){
                    return <Badge color={'cyan'} text={t['statList.columns.state.frozen']}/>;
                }else if(value === StatStateEnum.FROZEN){
                    return <Badge color={'red'} text={t['statList.columns.state.frozen']}/>;
                }else if(value === StatStateEnum.INVALID){
                    return <Badge color={'red'} text={t['statList.columns.state.frozen']}/>;
                }
            },
        },
        {
            title: 'Operate',
            dataIndex: 'operate',
            headerCellStyle: {width:'200px' },
            render: (_, record) => (
                <Space size={16} direction="horizontal">
                    <Button
                        onClick={() => {callback(record,"showUpdateModal")}}
                        type="secondary"
                        size="mini">
                        {t['statList.table.operations.update']}
                    </Button>
                    <Popconfirm
                        position={"tr"}
                        focusLock
                        title='Confirm'
                        content={t['statList.table.operations.frozen.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.frozen']}
                        </Button>
                    </Popconfirm>
                    <Popconfirm
                        position={"tr"}
                        focusLock
                        title='Confirm'
                        content={t['statList.table.operations.delete.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.delete']}
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];
}


export function getColumns(t: any, callback: (record: Record<string, any>, type: string) => Promise<void>) {

    return [
        {
            title: '',
            dataIndex: 'binded',
            headerCellStyle: { width:'20px' },
            render: (_, record) => {
                return <Button icon={<PiLinkSimple/>} size={"mini"} shape={"round"} onClick={() => {callback(record, 'binded')}}/>
            }
        },
        {
            title: 'ID',
            dataIndex: 'id',
        },
        {
            title: 'Title',
            dataIndex: 'title',
            render:(value,record) => {
                return (<div onClick={() => callback(record, 'detail')} style={{ cursor: "pointer" }} ><Text>{value}</Text></div>)
            }
        },
        {
            title: 'Project',
            dataIndex: 'project.title',
        },
        {
            title: 'Department',
            dataIndex: 'department.name',
        },
        {
            title: 'Admins',
            dataIndex: 'adminIds',
            render:(value,record) => {
                return (
                    record.admins.map((user) => (
                        <Popover
                            key={user.id}
                            trigger='click'
                            content={
                                <span>
                                <div><b>Email：</b>{user.email}</div>
                                <div><b>Phone：</b>{user.phone}</div>
                              </span>
                            }>
                            <Text style={{ cursor:"pointer" }}>
                                {user.userName};
                            </Text>
                        </Popover>
                        ))
                )
            }
        },
        {
            title: 'State',
            dataIndex: 'state',
            render: (value) => {
                if(value === 0){
                    return <Badge status="processing" text={t['statList.columns.state.pending']}/>;
                }else if (value === 1) {
                    return <Badge status="success" text={t['statList.columns.state.running']}/>;
                }else if(value === 2){
                    return <Badge status="error" text={t['statList.columns.state.limiting']}/>;
                }else if(value === 3){
                    return <Badge status="error" text={t['statList.columns.state.frozen']}/>;
                }
            },
        },
        {
            title: 'Operate',
            dataIndex: 'operate',
            headerCellStyle: {width:'200px' },
            render: (_, record) => (
                <Space size={16} direction="horizontal">
                    <Button
                        type="secondary"
                        size="mini">
                        {t['statList.table.operations.view']}
                    </Button>
                    <Button
                        type="secondary"
                        size="mini">
                        {t['statList.table.operations.update']}
                    </Button>
                    <Popconfirm
                        position={"tr"}
                        focusLock
                        title='Confirm'
                        content={t['statList.table.operations.frozen.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.frozen']}
                        </Button>
                    </Popconfirm>
                    <Popconfirm
                        position={"tr"}
                        focusLock
                        title='Confirm'
                        content={t['statList.table.operations.delete.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.delete']}
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];
}