import React, {useEffect, useMemo, useState} from 'react';
import {
  Radio,
  Button,
  Card,
  Grid,
  PaginationProps,
  Space,
  Table,
  Tabs,
  Typography,
  Modal, Divider, Steps,
} from '@arco-design/web-react';
import PermissionWrapper from '@/components/PermissionWrapper';
import {IconDownload, IconPlus} from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import SearchForm from './form';
import locale from './locale';
import styles from './style/index.module.less';
import '../mock';
import {getColumns} from './constants';
import {requestQueryList} from "@/api/project";
import {ResultData} from "@/types/insights-common";
import {PrivilegeEnum, Project, ProjectPagination} from "@/types/insights-web";
import {requestPrivilegeCheck} from "@/api/privilege";
import {getDataWithLocalCache} from "@/utils/localCache";
import {fetchAllData as fetchAllDepartmentData} from "@/pages/department/common";
import InfoForm from "@/pages/user/setting/info";
import Security from "@/pages/user/setting/security";
import useForm from "@arco-design/web-react/es/Form/useForm";

const { Title } = Typography;

function ProjectList() {
  const t = useLocale(locale);
  const tableCallback = async (record, type) => {
    console.log("record is:" + JSON.stringify(record));
    console.log(record, type);
  };
  const columns = useMemo(() => getColumns(t, tableCallback), [t]);
  const [data, setData] = useState([]);
  const [owner, setOwner] = useState(true);
  const [form] = useForm();
  const Step = Steps.Step;
  const [visible, setVisible] = React.useState(false);
  const [disabled, setDisabled] = React.useState(true);
  const [pagination, setPagination] = useState<PaginationProps>({
    sizeCanChange: true,
    showTotal: true,
    pageSize: 15,
    current: 1,
    pageSizeChangeResetCurrent: true,
  });
  const [loading, setLoading] = useState(true);
  const [formParams, setFormParams] = useState({});
  useEffect(() => {
    console.log("formParams is:" + JSON.stringify(formParams));
    setLoading(true);
    fetchDepartData().then().catch(error => {
      console.log("error:" + error)
    });
    fetchData().then().catch(error => {
      console.log("error:" + error)
    }).finally(() => {
      setLoading(false);
    })
  }, [owner,pagination.current, pagination.pageSize, JSON.stringify(formParams)]);

  const fetchDepartData = async ():Promise<void> => {
    const departData = await getDataWithLocalCache('cache_all_department',300,fetchAllDepartmentData);

  }

  const fetchProjectsData = async ():Promise<ResultData<{list:Array<Project>,total:number}>> => {
    return new Promise((resolve) => {
       const proc = async () => {
         const {current, pageSize} = pagination;
         const result =  await requestQueryList({
           params: {
             page: current,
             pageSize,
             ...formParams,
             owner:owner?1:0,
           },
         });
         setPagination({
           ...pagination,
           current,
           pageSize,
           total: result.data.total,});
         resolve(result);
       }
       proc();
    });
  }

  const fetchPrivilegeData = async ({type,items}):Promise<ResultData> => {
    return new Promise((resolve) => {
      const proc = async () => {
        const result = await requestPrivilegeCheck({type:type,items:items});
        resolve(result);
      }
      proc();
    })
  }

  const combineListData = (p1:Array<Project>,p2:Record<string, Array<number>>) => {
    return  p1.reduce((result:ProjectPagination[],item:Project) => {
      const combinedItem = { ...item, ...{"permissions":p2[item.id]} };
      result.push(combinedItem);
      return result;
    },[])
  }

  const fetchData = async (): Promise<void> => {
    const projectData = await fetchProjectsData();
    const projectIds = projectData.data.list!.map(z => z.id);
    const privilegeData = await fetchPrivilegeData({type:"project",items:projectIds});
    const listData = combineListData(projectData.data.list,privilegeData.data);
    setData(listData);
  }

  function onChangeTable({ current, pageSize }) {
    setPagination({
      ...pagination,
      current,
      pageSize,
    });
  }

  function handleSearch(params) {
    setPagination({ ...pagination, current: 1 });
    setFormParams(params);
  }

  function handleReset(){
    form.resetFields();
    handleSearch({});
  }

  function onClickRadio(p){
    setOwner(p==1);
    handleReset();
  }

  return (
    <Card>
      <SearchForm onSearch={handleSearch} onClear={handleReset} form={form}/>
      <Grid.Row justify="space-between" align="center">
        <Grid.Col span={16} style={{ textAlign: 'left' }}>
          <Space>
            <Radio.Group defaultValue={'0'} name='button-radio-group' onChange={onClickRadio}>
              {[{value:"0",label:"全部工程"}, {value:"1",label:"我的工程"}].map((item) => {
                return (
                    <Radio key={item.value} value={item.value}>
                      {({ checked }) => {
                        return (
                            <Button tabIndex={-1} key={item.value} shape='round'
                              style={checked ? {color:'rgb(var(--primary-6)',fontWeight:500}:{fontWeight:500}}>
                              {item.label}
                            </Button>
                        );
                      }
                      }
                    </Radio>
                );
              })}
            </Radio.Group>

          </Space>
        </Grid.Col>
        <Grid.Col span={8} style={{ textAlign: 'right' }}>
          <Space>
            <Button size={"small"} type="primary" onClick={() => setVisible(true)}>创建</Button>
          </Space>
        </Grid.Col>
      </Grid.Row>

      <Table
          style={{ marginTop:12}}
          rowKey="id"
          size={"default"}
          loading={loading}
          onChange={onChangeTable}
          pagination={pagination}
          columns={columns}
          data={data}
      />

      <Modal
          title='Manage Plugins'
          visible={visible}
          className='modal-demo-without-content-spacing'
          onOk={() => setVisible(false)}
          onCancel={() => setVisible(false)}
      >
        <div style={{ padding: '16px 0' }}>
          <Steps size='small' lineless current={2} style={{ maxWidth: 375, margin: '0 auto' }}>
            <Step title='Succeeded' />
            <Step title='Processing' />
            <Step title='Pending' />
          </Steps>
        </div>
        <Divider style={{ margin: 0 }} />
        <div style={{ padding: '24px 20px' }}>
          <p>
            You can select multiple plugins for the current project so that our app will verify that
            the plugins are installed and enabled.
          </p>
          <p style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>List of plugins</p>

        </div>
      </Modal>
    </Card>
  );
}

export default ProjectList;
