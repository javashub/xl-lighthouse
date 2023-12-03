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
const TabPane = Tabs.TabPane;
const { Text } = Typography;
export function getColumnsOfManage(t: any, callback: (record: Record<string, any>, type: string) => Promise<void>) {

    return [
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
                        content={t['statList.table.operations.stop.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.stop']}
                        </Button>
                    </Popconfirm>
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


export function getColumns(t: any,favoriteIds:Array<number>, callback: (record: Record<string, any>, type: string) => Promise<void>) {

    return [
        {
            title: '',
            dataIndex: 'favorite',
            headerCellStyle: { width:'5px' },
            render: (_, record) => {
                if(favoriteIds?.includes(record.id)){
                    return <Space size={16} direction="horizontal">
                        <Popconfirm
                            focusLock
                            position={"bl"}
                            title='Confirm'
                            content= {t['statList.table.operations.unfavorite.confirm']}
                            onOk={() => callback(record, 'unFavorite')}
                        >
                            <IconStarFill style={{ cursor:"pointer"}}/>
                        </Popconfirm>
                    </Space>
                }else{
                    return <Space size={16} direction="horizontal">
                        <Popconfirm
                            focusLock
                            position={"bl"}
                            title='Confirm'
                            content= {t['statList.table.operations.favorite.confirm']}
                            onOk={() => callback(record, 'favorite')}
                        >
                            <IconStar style={{ cursor:"pointer" }}/>
                        </Popconfirm>
                    </Space>
                }
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
            dataIndex: 'project.name',
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
                        content={t['statList.table.operations.stop.confirm']}
                    >
                        <Button
                            type="secondary"
                            size="mini">
                            {t['statList.table.operations.stop']}
                        </Button>
                    </Popconfirm>
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