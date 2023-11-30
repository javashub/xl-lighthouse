import React from 'react';
import {
  Avatar,
  Descriptions,
} from '@arco-design/web-react';
import {IconUser} from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './style/header.module.less';

export default function Header({userInfo}) {

  const t = useLocale(locale);

  return (
    <div className={styles['info-wrapper']}>
        <Avatar
            size={100}
            style={{ backgroundColor: 'rgb(123 187 221)' }}
        >
            <IconUser/>
        </Avatar>
      <Descriptions
    className={styles['info-content']}
    column={2}
    colon="："
    labelStyle={{textAlign: 'right'}}
    data={[
        {
            label: t['userSetting.label.name'],
            value:  userInfo.userName,
        },
        {
            label: t['userSetting.label.accountId'],
            value: userInfo.id,
        },
        {
            label: t['userSetting.label.phoneNumber'],
            value: (<span>
                {userInfo.phone}
              </span>),
        },
        {
            label: t['userSetting.label.department'],
            value: (<span>
                {userInfo.departmentName}
              </span>),
        },
        {
            label: t['userSetting.label.email'],
            value: (<span>
                {userInfo.email}
              </span>),
        },
        {
            label: t['userSetting.label.registrationTime'],
            value: userInfo.createdTime,
        },
    ]}
    />
    </div>
  );
}
