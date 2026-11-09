import { useEffect, useRef, useState } from 'react';
import { Button, Card, Descriptions, Drawer, Form, Image, Input, InputNumber, Modal, Space, Table, Tabs, Tag, message } from 'antd';
import { get, post } from '@/services/api';
import type { PageData } from '@/types/common';

type Row = Record<string, any>;
type Field = [string, string];
const fields: Record<string, Field[]> = {
  pets: [['pet_name','宠物名称'],['pet_type','类型'],['breed','品种'],['gender','性别'],['birth_date','出生日期'],['weight_kg','体重(kg)'],['height_cm','身高(cm)'],['bio','简介']],
  diaries: [['pet_id','宠物ID'],['diary_date','日记日期'],['category','分类'],['morning_mood','心情'],['content','内容'],['is_published','公开状态']],
  posts: [['pet_id','宠物ID'],['title','标题'],['content','内容'],['status','发布状态'],['like_count','点赞数'],['comment_count','评论数']],
  comments: [['post_id','帖子ID'],['parent_comment_id','父评论ID'],['content','评论内容'],['like_count','点赞数']],
  weight: [['pet_id','宠物ID'],['weight_kg','体重(kg)'],['recorded_at','记录时间']],
  feeding: [['pet_id','宠物ID'],['feed_type','餐次'],['amount_g','份量(g)'],['meal_time','喂食时间'],['notes','备注']],
  vaccine: [['pet_id','宠物ID'],['vaccine_name','疫苗名称'],['vaccinated_at','接种日期'],['next_due_at','下次接种'],['vet_clinic','医院'],['notes','备注']],
  checkup: [['pet_id','宠物ID'],['checkup_date','体检日期'],['vet_clinic','医院'],['findings','体检发现'],['recommendations','医生建议']],
};
const labels: Record<string,string> = { daily:'日常',food:'饮食',health:'健康',happy:'开心',calm:'平静',tired:'有点累',sad:'不开心',breakfast:'早饭',dinner:'晚饭',lunch:'午饭',snack:'加餐',cat:'猫',dog:'狗' };
function display(key: string, value: any) {
  if(value === null || value === undefined || value === '') return '—';
  if(key === 'status' || key === 'is_published') return <Tag color={Number(value) === 1 ? 'green' : 'default'}>{Number(value) === 1 ? '已公开' : '未公开'}</Tag>;
  if(key === 'gender') return ['未知','公','母'][Number(value)] || '未知';
  return labels[String(value)] || String(value);
}
function photos(row: Row): string[] {
  if(row.avatar_url) return [row.avatar_url];
  try { const parsed=JSON.parse(row.images_json || '[]'); return Array.isArray(parsed) ? parsed.filter((s: unknown)=>typeof s === 'string') : []; } catch { return []; }
}
export function RecordsPanel({ kind, initialId }: { kind: string; initialId?: number }) {
  const [form] = Form.useForm();
  const [reasonForm] = Form.useForm();
  const [query,setQuery] = useState<Row>(initialId ? {recordId:initialId} : {});
  const [page,setPage] = useState(1);
  const [size,setSize] = useState(20);
  const [data,setData] = useState<PageData<Row>>();
  const [loading,setLoading] = useState(false);
  const [revision,setRevision] = useState(0);
  const [detail,setDetail] = useState<Row>();
  const [logs,setLogs] = useState<Row[]>([]);
  const [operation,setOperation] = useState<{row:Row;action:string}>();
  const [saving,setSaving] = useState(false);
  const sequence=useRef(0);
  const detailSequence=useRef(0);
  useEffect(()=>{
    const seq=++sequence.current; setLoading(true);
    get<PageData<Row>>(`/api/admin/data/${kind}`,{params:{...query,page,size}})
      .then(result=>{if(seq===sequence.current)setData(result);})
      .catch(error=>{if(seq===sequence.current)message.error(error.message);})
      .finally(()=>{if(seq===sequence.current)setLoading(false);});
    return ()=>{sequence.current++;};
  },[kind,query,page,size,revision]);
  const openDetail=async(row:Row)=>{
    const seq=++detailSequence.current;
    try {
      const [record,history]=await Promise.all([get<Row>(`/api/admin/data/${kind}/${row.id}`),get<Row[]>('/api/admin/moderation/logs',{params:{targetType:kind,targetId:row.id}})]);
      if(seq===detailSequence.current){setDetail({...row,...record});setLogs(history);}
    }catch(error:any){message.error(error.message);}
  };
  const moderate=async()=>{
    const values=await reasonForm.validateFields(); if(!operation)return;
    setSaving(true);
    try { await post(`/api/admin/data/${kind}/${operation.row.id}/moderate`,{action:operation.action,...values});message.success('管理操作已记录');setOperation(undefined);setRevision(v=>v+1);setDetail(undefined); }
    catch(error:any){message.error(error.message);}finally{setSaving(false);}
  };
  const columns:any[]=[{title:'ID',dataIndex:'id',width:80},{title:'所属用户',width:150,render:(_:any,r:Row)=>`${r.owner_name || '未知用户'} (#${r.owner_id ?? '—'})`},
    ...fields[kind].slice(0,kind==='pets'?5:4).map(([key,label])=>({title:label,dataIndex:key,ellipsis:true,render:(value:any)=>display(key,value)})),
    {title:'操作',fixed:'right',width:210,render:(_:any,row:Row)=><Space wrap><Button type="link" onClick={()=>openDetail(row)}>详情</Button>{['posts','diaries','comments'].includes(kind)&&<Button type="link" danger onClick={()=>{reasonForm.resetFields();setOperation({row,action:kind==='comments'?'remove':'hide'});}}>{kind==='comments'?'移除':'下架'}</Button>}{kind==='posts'&&Number(row.status)===0&&<Button type="link" onClick={()=>{reasonForm.resetFields();setOperation({row,action:'publish'});}}>恢复发布</Button>}</Space>}];
  return <>
    <Form form={form} layout="inline" onFinish={values=>{setQuery(values);setPage(1);}} style={{marginBottom:20,rowGap:12}}>
      <Form.Item name="recordId" initialValue={initialId}><InputNumber min={1} precision={0} placeholder="记录ID" /></Form.Item>
      <Form.Item name="keyword"><Input placeholder="用户名或内容关键字" allowClear /></Form.Item>
      <Form.Item name="userId"><InputNumber min={1} precision={0} placeholder="用户ID" /></Form.Item>
      {kind!=='comments'&&<Form.Item name="petId"><InputNumber min={1} precision={0} placeholder="宠物ID" /></Form.Item>}
      {kind==='comments'&&<Form.Item name="postId"><InputNumber min={1} precision={0} placeholder="帖子ID" /></Form.Item>}
      <Button type="primary" htmlType="submit">查询</Button><Button onClick={()=>{form.resetFields();setQuery({});setPage(1);}}>重置</Button><Button onClick={()=>setRevision(v=>v+1)}>刷新</Button>
    </Form>
    <Table rowKey="id" loading={loading} columns={columns} dataSource={data?.records || []} scroll={{x:1100}} pagination={{current:page,pageSize:size,total:data?.total || 0,showSizeChanger:true,onChange:(p,s)=>{setPage(p);setSize(s);}}}/>
    <Drawer title="记录详情" open={!!detail} onClose={()=>{detailSequence.current++;setDetail(undefined);}} size="large">
      {detail&&<><Descriptions bordered column={1} items={[[ 'id','记录ID'],['owner_id','所属用户ID'],['owner_name','所属用户名'],...fields[kind],['created_at','创建时间'],['updated_at','更新时间']].map(([key,label])=>({key,label,children:display(key,detail[key])}))}/><Image.PreviewGroup>{photos(detail).map(src=><Image key={src} src={src} width={140} style={{margin:8}}/>)}</Image.PreviewGroup>{logs.length>0&&<Card title="管理操作记录" style={{marginTop:20}}>{logs.map(log=><p key={log.id}>{log.created_at} · 管理员 #{log.admin_id} · {log.action}：{log.reason}</p>)}</Card>}</>}
    </Drawer>
    <Modal title={operation?.action==='publish'?'恢复发布':operation?.action==='remove'?'移除评论':'下架内容'} open={!!operation} onCancel={()=>setOperation(undefined)} onOk={moderate} confirmLoading={saving} destroyOnHidden><Form form={reasonForm} layout="vertical"><Form.Item label="操作原因（留存审计）" name="reason" rules={[{required:true,whitespace:true,message:'请填写原因'}]}><Input.TextArea maxLength={500} showCount rows={3}/></Form.Item></Form></Modal>
  </>;
}
export default function DataManagementPage({section}:{section:'pets'|'diaries'|'community'|'health'}) {
  const groups:Record<string,[string,string][]>={pets:[['pets','宠物档案']],diaries:[['diaries','日记记录']],community:[['posts','帖子管理'],['comments','评论管理']],health:[['weight','体重记录'],['feeding','饮食记录'],['vaccine','疫苗记录'],['checkup','体检记录']]};
  const titles={pets:'宠物管理',diaries:'日记管理',community:'社区内容管理',health:'健康档案管理'};
  return <Card title={titles[section]}><Tabs destroyOnHidden items={groups[section].map(([key,label])=>({key,label,children:<RecordsPanel kind={key}/>}))}/></Card>;
}
