import { useEffect, useState } from 'react';
import { Alert, Button, Card, Input, Select, Space, Switch, message } from 'antd';
import { get, post, put } from '@/services/api';

const path = '/api/admin/assistant/instructions';
const scenes = { SHOP: '商品推荐', CARE: '日常养护', HEALTH: '健康咨询' };
const fields = { SPECIES: '物种', AGE: '年龄', BREED: '品种', WEIGHT: '体重' };
type Scene = keyof typeof scenes;
type Field = keyof typeof fields;
type Missing = 'ASK' | 'GENERAL' | 'OMIT';
type Config = { instruction: string; rules: Record<Scene, Partial<Record<Field, Missing>>> };
type Settings = { draft: Config; published: Config; revision: number; canRollback: boolean };
type Preview = { scene: Scene; profile: Partial<Record<Field, string>>; instructions: string; answer: string };
const sceneKeys = Object.keys(scenes) as Scene[];
const fieldKeys = Object.keys(fields) as Field[];

export default function AssistantInstructionsPage() {
  const [loadError, setLoadError] = useState('');
  const [settings, setSettings] = useState<Settings>();
  const [draft, setDraft] = useState<Config>();
  const [busy, setBusy] = useState(false);
  const [question, setQuestion] = useState('推荐 100 元以内的零食');
  const [sample, setSample] = useState(true);
  const [preview, setPreview] = useState<Preview>();

  function accept(value: Settings) {
    setSettings(value);
    setDraft(value.draft);
  }

  useEffect(() => {
    get<Settings>(path).then(accept).catch(error => setLoadError(error.message || '读取配置失败'));
  }, []);

  const dirty = JSON.stringify(draft) !== JSON.stringify(settings?.draft);
  async function run(action: () => Promise<void>) {
    setBusy(true);
    try {
      await action();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      setBusy(false);
    }
  }

  function changeRule(scene: Scene, field: Field, value: Missing | 'OFF') {
    if (!draft) return;
    const rules = { ...draft.rules[scene] };
    if (value === 'OFF') delete rules[field];
    else rules[field] = value;
    setDraft({ ...draft, rules: { ...draft.rules, [scene]: rules } });
  }

  if (!settings || !draft) return <Card title="提示词与宠物档案">{loadError ? <Alert type="error" message={loadError} /> : "正在读取配置…"}</Card>;
  return (
    <Card title="提示词与宠物档案" extra={`生效版本 ${settings.revision || '默认'}`}>
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" message="保存草稿后可用示例问题预览。发布后，新一轮助手回复使用该配置。" />
        <label htmlFor="assistant-instruction">补充提示词</label>
        <Input.TextArea id="assistant-instruction" rows={5} maxLength={4000} showCount disabled={busy}
          value={draft.instruction} placeholder="例如：回答简短，先给结论。"
          onChange={event => setDraft({ ...draft, instruction: event.target.value })} />
        {sceneKeys.map(scene => (
          <Card key={scene} size="small" title={scenes[scene]}>
            <Space wrap>
              {fieldKeys.map(field => (
                <label key={field}>
                  {fields[field]}{' '}
                  <Select<Missing | 'OFF'> aria-label={scenes[scene] + fields[field]} style={{ width: 145 }}
                    disabled={busy} value={draft.rules[scene][field] ?? 'OFF'}
                    onChange={value => changeRule(scene, field, value)}
                    options={[
                      { value: 'OFF', label: '不读取' },
                      { value: 'ASK', label: '缺失时按需追问' },
                      { value: 'GENERAL', label: '缺失时通用回答' },
                      { value: 'OMIT', label: '缺失时跳过' },
                    ]} />
                </label>
              ))}
            </Space>
          </Card>
        ))}
        <Space wrap>
          <Button disabled={busy || !dirty} onClick={() => run(async () => {
            accept(await put<Settings>(path + '/draft', draft));
            setPreview(undefined);
            message.success('草稿已保存');
          })}>保存草稿</Button>
          <Button type="primary" disabled={busy || dirty || JSON.stringify(settings.draft) === JSON.stringify(settings.published)}
            onClick={() => run(async () => {
              accept(await post<Settings>(path + '/publish'));
              message.success('已发布');
            })}>发布草稿</Button>
          <Button disabled={busy || dirty || !settings.canRollback} onClick={() => run(async () => {
            accept(await post<Settings>(path + '/rollback'));
            message.success('已恢复上一版');
          })}>恢复上一版</Button>
        </Space>
        <Card size="small" title="草稿预览">
          <Space orientation="vertical" style={{ width: '100%' }}>
            <Input aria-label="示例问题" value={question} maxLength={500} onChange={event => setQuestion(event.target.value)} />
            <Space><Switch checked={sample} onChange={setSample} />使用 7 个月猫咪的示例档案</Space>
            <Button disabled={busy || dirty || !question.trim()} loading={busy}
              onClick={() => run(async () => setPreview(await post<Preview>(path + '/preview', {
                question, withSamplePet: sample,
              }, { timeout: 25000 })))}>预览回复</Button>
            {preview && <>
              <div>{scenes[preview.scene]} · {fieldKeys.filter(field => preview.profile[field]).map(field =>
                fields[field] + ' ' + preview.profile[field]).join(' · ') || '未传入档案字段'}</div>
              <div style={{ whiteSpace: 'pre-wrap' }}>{preview.answer}</div>
              <details><summary>查看发送给模型的提示词</summary><pre style={{ whiteSpace: 'pre-wrap' }}>{preview.instructions}</pre></details>
            </>}
          </Space>
        </Card>
      </Space>
    </Card>
  );
}
