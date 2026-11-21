import { request } from './request';

interface RecorderEvents {
  frame: (event: { frameBuffer: ArrayBuffer }) => void;
  started: () => void;
  stopped: () => void;
  error: () => void;
}

const recorderBindings = new WeakMap<object, { active?: RecorderEvents }>();

/** 部分微信运行环境只有 on 接口，全局录音器只注册一组转发监听。 */
function recorderBinding(recorder: ReturnType<typeof wx.getRecorderManager>) {
  let binding = recorderBindings.get(recorder);
  if (!binding) {
    binding = {};
    recorderBindings.set(recorder, binding);
    const target = binding;
    recorder.onFrameRecorded((event: { frameBuffer: ArrayBuffer }) => target.active?.frame(event));
    recorder.onStart(() => target.active?.started());
    recorder.onStop(() => target.active?.stopped());
    recorder.onError(() => target.active?.error());
  }
  return binding;
}

/** 每次录音使用新的短效签名，密钥保留在后端。 */
export class StreamingSpeech {
  private socket?: ReturnType<typeof wx.connectSocket>;
  private recorder = wx.getRecorderManager();
  private ended = false;
  private segments = new Map<number, string>();
  private timer?: ReturnType<typeof setTimeout>;
  private ready = false;
  private stopping = false;
  private captureStarting = false;
  private endSent = false;
  private stopTimer?: ReturnType<typeof setTimeout>;
  private socketOpen = false;
  private socketClosed = false;
  private closeRequested = false;
  private binding?: { active?: RecorderEvents };
  private events: RecorderEvents = {
    frame: event => this.frame(event),
    started: () => this.started(),
    stopped: () => this.stopped(),
    error: () => this.recorderError(),
  };

  constructor(private onText: (text: string) => void,
    private onDone: (error?: string) => void,
    private onLevel: (level: number) => void = () => {},
    private onRecording: () => void = () => {}) {}

  async start() {
    const url = await request<string>({ url: '/assistant/speech/connection?voiceFormat=1', method: 'POST' });
    if (this.ended) return;
    this.binding = recorderBinding(this.recorder);
    if (this.binding.active) throw new Error('录音器正在结束上次录音，请稍后重试');
    this.binding.active = this.events;
    this.socket = wx.connectSocket({ url, success: () => {} });
    this.socket.onOpen(() => {
      this.socketOpen = true;
      // 取消可能发生在底层任务建立之前，等 onOpen 后再关闭。
      if (this.ended) this.closeSocket();
    });
    this.timer = setTimeout(() => this.finish('语音连接超时，请重试'), 10000);
    this.socket.onMessage((event: { data: string | ArrayBuffer }) => {
      if (this.ended) return;
      try {
        const message = JSON.parse(String(event.data));
        if (message.code != null && message.code !== 0) { this.finish('语音识别失败，请重试'); return; }
        if (message.result) {
          this.segments.set(message.result.index, message.result.voice_text_str || '');
          this.onText([...this.segments.entries()].sort((a, b) => a[0] - b[0]).map(([, text]) => text).join(''));
        }
        if (message.final === 1) { this.finish(); return; }
        if (!this.ready) {
          this.ready = true;
          clearTimeout(this.timer);
          this.timer = setTimeout(() => this.stop(), 60000);
          this.captureStarting = true;
          this.recorder.start({ duration: 60000, sampleRate: 16000, numberOfChannels: 1,
            format: 'PCM', frameSize: 6 });
        }
      } catch (_) { this.finish('语音响应异常，请重试'); }
    });
    this.socket.onError(() => this.finish('语音连接失败，请检查网络'));
    this.socket.onClose(() => {
      this.socketClosed = true;
      this.socketOpen = false;
      if (!this.ended) this.finish('语音连接已断开');
    });
  }

  private frame = (event: { frameBuffer: ArrayBuffer }) => {
    if (this.ended || this.endSent) return;
    this.onLevel(pcmLevel(event.frameBuffer));
    if (!this.ended) this.socket?.send({
      data: event.frameBuffer, fail: () => this.finish('语音发送失败，请重试'),
    });
  };
  private started = () => {
    this.captureStarting = false;
    if (this.ended || this.stopping) {
      this.recorder.stop();
    }
    else this.onRecording();
  };
  private stopped = () => {
    if (this.ended) { this.releaseRecorder(); return; }
    if (this.endSent) return;
    this.endSent = true;
    clearTimeout(this.stopTimer);
    clearTimeout(this.timer);
    this.stopping = true;
    this.socket?.send({ data: JSON.stringify({ type: 'end' }), fail: () => this.finish('语音发送失败') });
    this.timer = setTimeout(() => this.finish(this.segments.size
      ? '已保留实时文字，请核对末尾内容' : '没有收到识别结果，请重试'), 2500);
  };
  private recorderError = () => {
    this.captureStarting = false;
    this.releaseRecorder();
    this.finish('录音失败，请检查麦克风权限');
  };

  private releaseRecorder() {
    if (this.binding?.active === this.events) this.binding.active = undefined;
  }

  stop() {
    if (this.ended || this.stopping) return;
    if (!this.ready) { this.finish(); return; }
    this.stopping = true;
    // 部分设备原生停止事件较迟，避免界面停留到录音上限。
    this.stopTimer = setTimeout(() => this.stopped(), 1000);
    if (!this.captureStarting) this.recorder.stop();
  }

  cancel() { this.finish(); }

  private closeSocket() {
    if (!this.socket || !this.socketOpen || this.socketClosed || this.closeRequested) return;
    this.closeRequested = true;
    try {
      this.socket.close({
        success: () => { this.socketClosed = true; },
        // 远端先关闭或微信已释放底层任务时，关闭失败也已达到清理目的。
        fail: () => { this.socketClosed = true; },
      });
    } catch (_) {
      this.socketClosed = true;
    }
  }

  private finish(error?: string) {
    if (this.ended) return;
    this.ended = true;
    clearTimeout(this.timer);
    clearTimeout(this.stopTimer);
    try {
      if (this.ready) this.recorder.stop();
    } finally {
      // 原生录音仍在异步启动时，保留转发对象直至 onStart/onStop 收尾。
      if (!this.captureStarting) this.releaseRecorder();
      try { this.closeSocket(); }
      finally { this.onDone(error); }
    }
  }
}

/** 16bit little-endian PCM 的 RMS，映射为0至1的相对音量。 */
export function pcmLevel(buffer: ArrayBuffer): number {
  const samples = Math.floor(buffer.byteLength / 2);
  if (!samples) return 0;
  const view = new DataView(buffer);
  let energy = 0;
  for (let i = 0; i < samples; i++) {
    const sample = view.getInt16(i * 2, true) / 32768;
    energy += sample * sample;
  }
  const db = 20 * Math.log10(Math.max(Math.sqrt(energy / samples), 0.00001));
  return Math.max(0, Math.min(1, (db + 55) / 50));
}
