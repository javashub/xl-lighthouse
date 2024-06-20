import React, {useContext, useEffect, useRef, useState} from 'react';
import dayjs from 'dayjs';
import {
    Form,
    Grid, TreeSelect, Drawer, Descriptions, Divider, Link,
} from '@arco-design/web-react';
import {getStatExpiredEnumDescription} from "@/pages/common/desc/base";
const { Row, Col } = Grid;
const { useForm } = Form;

export default function StatDetailModal({statInfo,onClose}) {

    const [loading,setLoading] = useState(true);



    return <div>
        <Drawer
            width= {'50%'}
            title={<span>Statistic Information</span>}
            visible={true}
            placement={"right"}
            onCancel={() => {
                onClose();
            }}
            footer={null}
        >
            <Descriptions
                colon='：'
                column={1}
                labelStyle={{ width: 100 }}
                data={[
                    {
                        label: 'Title',
                        value: statInfo?.title,
                        span:1,
                    },
                    {
                        label: 'Expired',
                        value: getStatExpiredEnumDescription(statInfo?.expired),
                        span:1,
                    },
                    {
                        label: 'TimeParam',
                        value: statInfo?.timeparam,
                        span:1,
                    },
                    {
                        label: 'Template',
                        value: statInfo?.template,
                        span:3,
                    },
                    {
                        label: 'Description',
                        value: statInfo?.desc,
                        span:3,
                    },
                ]}
            />
        </Drawer>
    </div>
}