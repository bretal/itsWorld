import { useEffect, useRef, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import { get, post, put } from '@/services/api';
type Row=Record<string,any>;
export default function MemberCenterAdminTab({kind}:{kind:'subscription-plans'|'reward-products'|'redemptions'}) {
  const [rows,setRows]=useState<Row[]>([]),[levels,setLevels]=useState<Row[]>([]);
  const [loading,setLoading]=useState(false),[saving,setSaving]=useState(false),[open,setOpen]=useState(false);
  const [editing,setEditing]=useState<Row>(),[revision,setRevision]=useState(0),[page,setPage]=useState(1),[userId,setUserId]=useState<number>();
  const [filterUserId,setFilterUserId]=useState<number>();
  const [form]=Form.useForm(); const sequence=useRef(0);const redeem=kind==='redemptions',plan=kind==='subscription-plans';
  useEffect(()=>{const seq=++sequence.current;setLoading(true);
    Promise.all([get<Row[]>(`/api/admin/member/${kind}`,{params:{page,size:20,userId:filterUserId}}),plan?get<Row[]>('/api/admin/member/levels'):Promise.resolve([])])
    .then(([data,options])=>{if(seq===sequence.current){setRows(data);setLevels(options);}}).catch(error=>message.error(error.message)).finally(()=>{if(seq===sequence.current)setLoading(false);});
    return ()=>{sequence.current++;};
  },[kind,page,filterUserId,revision,plan]);
  const edit=(row?:Row)=>{setEditing(row);form.resetFields();form.setFieldsValue(row?{...row,levelId:row.level_id,durationDays:row.duration_days,growthCost:row.growth_cost}:{status:1,stock:0,durationDays:365});setOpen(true);};
  const save=async()=>{const values=await form.validateFields();setSaving(true);try{if(editing)await put(`/api/admin/member/${kind}/${editing.id}`,values);else await post(`/api/admin/member/${kind}`,values);message.success('保存成功');setOpen(false);setRevision(v=>v+1);}catch(error:any){message.error(error.message);}finally{setSaving(false);}};
  const fulfill=async(row:Row)=>{setSaving(true);try{await post(`/api/admin/member/redemptions/${encodeURIComponent(row.redemption_no)}/fulfill`);message.success('核销成功');setRevision(v=>v+1);}catch(error:any){message.error(error.message);}finally{setSaving(false);}};
  const columns:any[]=redeem?[
    {title:'兑换单号',dataIndex:'redemption_no'}, {title:'用户ID',dataIndex:'user_id'}, {title:'礼品',dataIndex:'name_snapshot'}, {title:'消耗成长值',dataIndex:'growth_cost'},
    {title:'状态',dataIndex:'status',render:(v:string)=><Tag color={v==='PENDING'?'orange':'green'}>{v==='PENDING'?'待核销':v==='FULFILLED'?'已核销':v}</Tag>},
    {title:'兑换时间',dataIndex:'created_at'}, {title:'核销人',dataIndex:'fulfilled_by'}, {title:'核销时间',dataIndex:'fulfilled_at'},
    {title:'操作',render:(_:any,r:Row)=>r.status==='PENDING'?<Popconfirm title="确认礼品已交付，执行核销？" onConfirm={()=>fulfill(r)}><Button type="link" disabled={saving}>确认核销</Button></Popconfirm>:null}
  ]:[{title:'ID',dataIndex:'id'}, {title:'名称',dataIndex:'name'}, ...(plan?[
    {title:'会员等级',dataIndex:'level_id',render:(v:number)=>levels.find(l=>Number(l.id)===Number(v))?.levelName || levels.find(l=>Number(l.id)===Number(v))?.level_name || v},
    {title:'价格（元）',dataIndex:'price',render:(v:number)=>(v/100).toFixed(2)}, {title:'有效天数',dataIndex:'duration_days'}
  ]:[{title:'所需成长值',dataIndex:'growth_cost'},{title:'库存',dataIndex:'stock'}]),
    {title:'状态',dataIndex:'status',render:(v:number)=><Tag color={v===1?'green':'default'}>{v===1?'启用':'停用'}</Tag>}, {title:'操作',render:(_:any,r:Row)=><Button type="link" onClick={()=>edit(r)}>编辑</Button>}];
  return <><Space style={{marginBottom:16}} wrap>{!redeem?<Button type="primary" onClick={()=>edit()}>新增{plan?'会员方案':'兑换商品'}</Button>:<><InputNumber min={1} precision={0} placeholder="用户ID" value={userId} onChange={v=>setUserId(v??undefined)}/><Button onClick={()=>{setFilterUserId(userId);setPage(1);}}>查询</Button><Button onClick={()=>{setUserId(undefined);setFilterUserId(undefined);setPage(1);}}>重置</Button></>}<Button onClick={()=>setRevision(v=>v+1)}>刷新</Button></Space>
    <Table rowKey="id" dataSource={redeem?rows.slice(0,20):rows} columns={columns} loading={loading} scroll={{x:1000}} pagination={redeem?false:{pageSize:20}}/>
    {redeem&&<Space><Button disabled={page===1||loading} onClick={()=>setPage(p=>p-1)}>上一页</Button><span>第{page}页</span><Button disabled={rows.length<20||loading} onClick={()=>setPage(p=>p+1)}>下一页</Button></Space>}
    <Modal title={`${editing?'编辑':'新增'}${plan?'会员方案':'兑换商品'}`} open={open} onCancel={()=>setOpen(false)} onOk={save} confirmLoading={saving} destroyOnHidden>
      <Form form={form} layout="vertical"><Form.Item label="名称" name="name" rules={[{required:true,whitespace:true}]}><Input maxLength={100}/></Form.Item>
      {plan?<><Form.Item label="会员等级" name="levelId" rules={[{required:true}]}><Select options={levels.map(l=>({value:l.id,label:l.levelName || l.level_name}))}/></Form.Item><Form.Item label="价格（分，100分=1元）" name="price" rules={[{required:true}]}><InputNumber min={1} precision={0} max={100000000} style={{width:'100%'}}/></Form.Item><Form.Item label="有效天数" name="durationDays" rules={[{required:true}]}><InputNumber min={1} max={3650} precision={0}/></Form.Item></>:<><Form.Item label="所需成长值" name="growthCost" rules={[{required:true}]}><InputNumber min={1} precision={0}/></Form.Item><Form.Item label="库存" name="stock" rules={[{required:true}]}><InputNumber min={0} precision={0}/></Form.Item></>}
      <Form.Item label="状态" name="status" rules={[{required:true}]}><Select options={[{value:1,label:'启用'},{value:0,label:'停用'}]}/></Form.Item></Form>
    </Modal></>;
}
