import { subscribeAgent, resumeAgent, stopAgent, replyAgent } from '../../utils/agent-runner';

Component({
  data: { task: null, text: '', paused: true, running: false, reply: '' },
  lifetimes: {
    attached() {
      this._unsubscribe = subscribeAgent(progress => this.setData({ ...progress, task: progress.task || null }));
    },
    detached() { this._unsubscribe?.(); },
  },
  methods: {
    resume() { void resumeAgent(); },
    stop() { void stopAgent(); },
    input(e: any) { this.setData({ reply: e.detail.value }); },
    async reply() {
      const text = this.data.reply;
      this.setData({ reply: '' });
      await replyAgent(text);
    },
  },
});
