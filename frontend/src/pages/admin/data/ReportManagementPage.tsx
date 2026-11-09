import { useEffect, useRef, useState } from 'react';
import { Button, Card, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, message } from 'antd';
import { get, post } from '@/services/api';
import type { PageData } from '@/types/common';
import { RecordsPanel } from './DataManagementPage';
type Row=Record<string,any>;
const types=[{value:'posts',label:'帖子'},{value:'comments',label:'评论'},{value:'diaries',label:'日记'}];
export default function ReportManagementPage(){
 const [data,setData]=useState<PageData<Row>>(),[page,setPage]=useState(1),[status,setStatus]=useState<string>(),[loading,setLoading]=useState(false),[revision,setRevision]=useState(0);
 const [creating,setCreating]=useState(false),[handling,setHandling]=useState<Row>(),[saving,setSaving]=useState(false),[view,setView]=useState<Row>();
 const [form]=Form.useForm(),[resolutionForm]=Form.useForm(); const sequence=useRef(0);
 useEffect(()=>{const seq=++sequence.current;setLoading(true);get<PageData<Row>>('/api/admin/moderation/reports',{params:{page,size:20,status}}).then(r=>{if(seq===sequence.current)setData(r);}).catch(e=>message.error(e.message)).finally(()=>{if(seq===sequence.current)setLoading(false);});return()=>{sequence.current++;};},[page,status,revision]);
 const save=async()=>{const values=await (creating?form:resolutionForm).validateFields();setSaving(true);try{await post(creating?'/api/admin/moderation/reports':`/api/admin/moderation/reports/${handling?.id}/resolve`,values);message.success('已保存');setCreating(false);setHandling(undefined);setRevision(v=>v+1);}catch(e:any){message.error(e.message);}finally{setSaving(false);}};
 return <Card title="举报处理"><Space style={{marginBottom:20}}><Select placeholder="处理状态" allowClear style={{width:150}} value={status} onChange={v=>{setStatus(v);setPage(1);}} options={[{value:'PENDING',label:'待处理'},{value:'RESOLVED',label:'已处理'},{value:'DISMISSED',label:'已驳回'}]}/><Button type="primary" onClick={()=>{form.resetFields();setCreating(true);}}>登记反馈</Button><Button onClick={()=>setRevision(v=>v+1)}>刷新</Button></Space>
 <Table rowKey="id" dataSource={data?.records} loading={loading} scroll={{x:1200}} pagination={{current:page,pageSize:20,total:data?.total,onChange:setPage}} columns={[
 {title:'工单ID',dataIndex:'id'},{title:'内容类型',dataIndex:'target_type',render:v=>types.find(t=>t.value===v)?.label||v},{title:'内容ID',dataIndex:'target_id'},{title:'反馈用户ID',dataIndex:'reporter_id'},{title:'举报原因',dataIndex:'reason'},
 {title:'状态',dataIndex:'status',render:v=><Tag>{({PENDING:'待处理',RESOLVED:'已处理',DISMISSED:'已驳回'} as Row)[v]||v}</Tag>},{title:'处理说明',dataIndex:'resolution'},{title:'处理人',dataIndex:'handled_by'},{title:'处理时间',dataIndex:'handled_at'},
 {title:'操作',render:(_,row)=><Space><Button type="link" onClick={()=>setView(row)}>查看内容</Button>{row.status==='PENDING'&&<Button type="link" onClick={()=>{resolutionForm.resetFields();setHandling(row);}}>处理</Button>}</Space>}
 ]}/>
 <Drawer title={view?`内容管理 · ${view.target_type} #${view.target_id}`:''} width="90%" open={!!view} onClose={()=>setView(undefined)}>{view&&<><p>核对目标内容后，可在详情中查看图片，并按原因下架或移除。</p><RecordsPanel key={view.id} kind={view.target_type} initialId={view.target_id}/></>}</Drawer>
 <Modal title={creating?'登记举报或外部反馈':'处理举报'} open={creating||!!handling} onCancel={()=>{setCreating(false);setHandling(undefined);}} onOk={save} confirmLoading={saving} destroyOnHidden>
 {creating?<Form form={form} layout="vertical"><Form.Item label="内容类型" name="targetType" rules={[{required:true}]}><Select options={types}/></Form.Item><Form.Item label="内容ID" name="targetId" rules={[{required:true}]}><InputNumber min={1} precision={0}/></Form.Item><Form.Item label="反馈用户ID（可选）" name="reporterId"><InputNumber min={1} precision={0}/></Form.Item><Form.Item label="举报原因" name="reason" rules={[{required:true,whitespace:true}]}><Input.TextArea maxLength={500}/></Form.Item></Form>:<Form form={resolutionForm} layout="vertical"><p>处理工单只记录结论；需要下架时，请先在“查看内容”中操作。</p><Form.Item label="处理结果" name="status" rules={[{required:true}]}><Select options={[{value:'RESOLVED',label:'已处理'},{value:'DISMISSED',label:'驳回'}]}/></Form.Item><Form.Item label="处理说明" name="resolution" rules={[{required:true,whitespace:true}]}><Input.TextArea maxLength={500}/></Form.Item></Form>}
 </Modal></Card>;
}
