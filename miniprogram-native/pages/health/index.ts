import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { listPets } from '../../services/pet';
import type { Pet } from '../../services/pet';
import {
  addCheckupRecord,
  addFeedingRecord,
  addVaccineRecord,
  addWeightRecord,
  deleteCheckupRecord,
  deleteFeedingRecord,
  deleteVaccineRecord,
  deleteWeightRecord,
  getCheckupRecordPage,
  getFeedingRecordPage,
  getVaccineRecordPage,
  getWeightRecordPage,
} from '../../services/health';
import type {
  CheckupRecord,
  FeedingRecord,
  VaccineRecord,
  WeightRecord,
} from '../../services/health';
import {
  formatDate,
  formatDateTime,
  parseDate,
  todayString,
} from '../../utils/format';
import { resolveImage } from '../../utils/image';

interface WeightItem extends WeightRecord {
  dateText?: string;
}

interface FeedingItem extends FeedingRecord {
  timeText?: string;
  mealLabel?: string;
}

interface VaccineItem extends VaccineRecord {
  vaccinatedText?: string;
  nextDueText?: string;
}

interface CheckupItem extends CheckupRecord {
  dateText?: string;
}

interface WeightBar {
  height: number;
  label: string;
  value: number;
  time: number;
}

const FEED_TYPE_LABELS: Record<string, string> = {
  breakfast: '早饭',
  lunch: '午饭',
  dinner: '晚饭',
  snack: '加餐',
};

Page(withStaticImages({
  data: {
    tabs: [
      { key: 'weight', label: '体重' },
      { key: 'feeding', label: '饮食' },
      { key: 'vaccine', label: '疫苗' },
      { key: 'checkup', label: '体检' },
    ],
    activeTab: 'weight',
    currentPet: null as Pet | null,
    petAvatarSrc: '',
    currentWeight: '--',
    loading: true,
    loadingMore: false,
    pageSize: 10,
    weightPage: 1,
    feedingPage: 1,
    vaccinePage: 1,
    checkupPage: 1,
    weightHasMore: true,
    feedingHasMore: true,
    vaccineHasMore: true,
    checkupHasMore: true,
    weightRecords: [] as WeightItem[],
    weightBars: [] as WeightBar[],
    chartWidth: 280,
    feedingRecords: [] as FeedingItem[],
    todayFeedingCount: 0,
    vaccineRecords: [] as VaccineItem[],
    checkupRecords: [] as CheckupItem[],
    today: '',
    sheetVisible: false,
    sheetType: '',
    sheetTitle: '',
    saving: false,
    weightValue: '',
    weightDate: '',
    feedingTypes: ['早饭', '晚饭'],
    feedingTypeValues: ['breakfast', 'dinner'],
    feedingTypeIndex: 0,
    feedingAmount: '',
    feedingTime: '',
    feedingNotes: '',
    vaccineName: '',
    vaccineDate: '',
    vaccineNextDate: '',
    vaccineClinic: '',
    vaccineNotes: '',
    checkupDate: '',
    checkupClinic: '',
    checkupFindings: '',
    checkupRecommendations: '',
  },

  onLoad() {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
    this.setData({ chartWidth: info.windowWidth - 64 });
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadPage();
  },

  onReachBottom() {
    const hasMoreKey = `${this.data.activeTab}HasMore`;
    if (
      !this.data[hasMoreKey] ||
      this.data.loading ||
      this.data.loadingMore
    ) {
      return;
    }
    this.loadTab(this.data.activeTab, undefined, true, false);
  },

  async loadPage() {
    this.setData({ loading: true });
    try {
      const pets = await listPets();
      const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
      const currentPet =
        (pets || []).find((pet) => pet.id === storedPetId) ||
        (pets || [])[0] ||
        null;

      if (!currentPet) {
        this.setData({ currentPet: null, loading: false });
        return;
      }

      const petAvatarSrc = await resolveImage(currentPet.avatarUrl);
      this.setData({
        currentPet,
        petAvatarSrc,
        currentWeight: currentPet.weight ? String(currentPet.weight) : '--',
      });
      await this.loadTab(this.data.activeTab, currentPet.id, true, true);
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadTab(tab: string, petId?: number, keepLoading?: boolean, reset = true) {
    const pet = this.data.currentPet;
    const id = petId || pet?.id;
    if (!id) {
      this.setData({ loading: false, loadingMore: false });
      return;
    }

    const pageKeyMap: Record<string, string> = {
      weight: 'weightPage',
      feeding: 'feedingPage',
      vaccine: 'vaccinePage',
      checkup: 'checkupPage',
    };
    const listKeyMap: Record<string, string> = {
      weight: 'weightRecords',
      feeding: 'feedingRecords',
      vaccine: 'vaccineRecords',
      checkup: 'checkupRecords',
    };
    const hasMoreKeyMap: Record<string, string> = {
      weight: 'weightHasMore',
      feeding: 'feedingHasMore',
      vaccine: 'vaccineHasMore',
      checkup: 'checkupHasMore',
    };

    const pageKey = pageKeyMap[tab];
    const listKey = listKeyMap[tab];
    const hasMoreKey = hasMoreKeyMap[tab];
    const page = reset ? 1 : (this.data[pageKey] || 1) + 1;

    if (reset && !keepLoading) {
      this.setData({ loading: true });
    } else if (!reset) {
      this.setData({ loadingMore: true });
    }

    try {
      if (tab === 'weight') {
        let result = await getWeightRecordPage(id, page, this.data.pageSize);
        const loaded = [...(result.records || [])];
        // 首屏补足最近七个记录日，避免同日多条记录挤掉趋势日期。
        if (reset) {
          while (new Set(loaded.map(record => record.recordedAt.slice(0, 10))).size < 7 && result.pageNum < result.totalPages) {
            result = await getWeightRecordPage(id, result.pageNum + 1, this.data.pageSize);
            loaded.push(...(result.records || []));
          }
        }
        result = { ...result, records: loaded };
        const items: WeightItem[] = (result.records || []).map((record) => ({
          ...record,
          dateText: formatDate(record.recordedAt),
        }));
        const records = reset
          ? items
          : (this.data.weightRecords as WeightItem[]).concat(items);
        this.setData({
          weightRecords: records,
          weightBars: this.buildBars(records),
          currentWeight:
            records.length && records[0].weightKg
              ? String(records[0].weightKg)
              : pet?.weight
                ? String(pet.weight)
                : '--',
          [pageKey]: result.pageNum,
          [hasMoreKey]: result.pageNum < result.totalPages,
        });
      } else if (tab === 'feeding') {
        const result = await getFeedingRecordPage(
          id,
          todayString(),
          page,
          this.data.pageSize,
        );
        const items: FeedingItem[] = (result.records || []).map((record) => ({
          ...record,
          timeText: formatDateTime(record.mealTime).slice(-5),
          mealLabel:
            FEED_TYPE_LABELS[record.feedType] || record.feedType || '喂食',
        }));
        this.setData({
          feedingRecords: reset
            ? items
            : (this.data.feedingRecords as FeedingItem[]).concat(items),
          todayFeedingCount: result.total,
          [pageKey]: result.pageNum,
          [hasMoreKey]: result.pageNum < result.totalPages,
        });
      } else if (tab === 'vaccine') {
        const result = await getVaccineRecordPage(id, page, this.data.pageSize);
        const items: VaccineItem[] = (result.records || []).map((record) => ({
          ...record,
          vaccinatedText: record.vaccinatedAt,
          nextDueText: record.nextDueAt || '',
        }));
        this.setData({
          vaccineRecords: reset
            ? items
            : (this.data.vaccineRecords as VaccineItem[]).concat(items),
          [pageKey]: result.pageNum,
          [hasMoreKey]: result.pageNum < result.totalPages,
        });
      } else {
        const result = await getCheckupRecordPage(id, page, this.data.pageSize);
        const items: CheckupItem[] = (result.records || []).map((record) => ({
          ...record,
          dateText: record.checkupDate,
        }));
        this.setData({
          checkupRecords: reset
            ? items
            : (this.data.checkupRecords as CheckupItem[]).concat(items),
          [pageKey]: result.pageNum,
          [hasMoreKey]: result.pageNum < result.totalPages,
        });
      }

      this.setData({ loading: false, loadingMore: false }, () => {
        if (tab === 'weight') wx.nextTick(() => this.drawTrend());
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  buildBars(records: WeightRecord[]): WeightBar[] {
    if (!records.length) {
      return [];
    }

    const days = new Map<string, WeightRecord>();
    records.slice().sort((a, b) =>
      (parseDate(a.recordedAt)?.getTime() || 0) - (parseDate(b.recordedAt)?.getTime() || 0) || a.id - b.id,
    ).forEach(record => {
      if (parseDate(record.recordedAt) && Number(record.weightKg) > 0) days.set(record.recordedAt.slice(0, 10), record);
    });
    const sorted = Array.from(days.values()).slice(-7);
    const values = sorted.map((item) => Number(item.weightKg) || 0);
    const max = Math.max(...values);
    const min = Math.min(...values);
    const range = Math.max(max - min, 0.5);

    return sorted.map((item) => ({
      height: 28 + Math.round(((Number(item.weightKg) - min) / range) * 64),
      label: (item.recordedAt || '').slice(5, 10).replace('-', '/'),
      value: Number(item.weightKg),
      time: parseDate(item.recordedAt)?.getTime() || 0,
    }));
  },

  drawTrend() {
    const points = this.data.weightBars as WeightBar[];
    if (points.length < 2 || this.data.activeTab !== 'weight') return;
    const ctx = wx.createCanvasContext('weightTrend', this);
    const width = this.data.chartWidth;
    const min = Math.max(0, Math.floor((Math.min(...points.map(p => p.value)) - 0.3) * 10) / 10);
    const max = Math.ceil((Math.max(...points.map(p => p.value)) + 0.3) * 10) / 10;
    const x = (p: WeightBar) => 38 + (p.time - points[0].time) / (points[points.length - 1].time - points[0].time) * (width - 62);
    const y = (p: WeightBar) => 155 - (p.value - min) / (max - min) * 125;
    ctx.clearRect(0, 0, width, 210);
    ctx.setFontSize(10);
    for (let i = 0; i < 3; i++) {
      const gy = 155 - i * 62.5;
      ctx.setStrokeStyle('#e8dced'); ctx.setLineWidth(1);
      ctx.beginPath(); ctx.moveTo(36, gy); ctx.lineTo(width - 16, gy); ctx.stroke();
      ctx.setFillStyle('#9b81a7'); ctx.fillText((min + (max - min) * i / 2).toFixed(1), 0, gy + 4);
    }
    ctx.setStrokeStyle('#ad7cd7'); ctx.setLineWidth(2);
    ctx.beginPath(); points.forEach((p, i) => i ? ctx.lineTo(x(p), y(p)) : ctx.moveTo(x(p), y(p))); ctx.stroke();
    points.forEach((p, i) => {
      ctx.setFillStyle('#ad7cd7'); ctx.beginPath(); ctx.arc(x(p), y(p), 3, 0, Math.PI * 2); ctx.fill();
      ctx.setTextAlign('center'); ctx.setFillStyle('#755482');
      if (points.length <= 4 || i % 2 === 0 || i === points.length - 1) {
        ctx.fillText(String(p.value), x(p), y(p) - 10);
        ctx.fillText(p.label, x(p), 180);
      }
    });
    ctx.draw();
  },

  switchTab(event: any) {
    const key = event.currentTarget.dataset.key;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key });
    this.loadTab(key);
  },

  openSheet(event: any) {
    const type = event.currentTarget.dataset.type;
    const today = todayString();
    const now = new Date();
    const time =
      String(now.getHours()).padStart(2, '0') +
      ':' +
      String(now.getMinutes()).padStart(2, '0');

    const titles: Record<string, string> = {
      weight: '记录体重',
      feeding: '添加喂食记录',
      vaccine: '添加疫苗记录',
      checkup: '添加体检记录',
    };

    this.setData({
      sheetVisible: true,
      sheetType: type,
      sheetTitle: titles[type] || '',
      weightValue:
        type === 'weight'
          ? this.data.currentWeight === '--'
            ? ''
            : this.data.currentWeight
          : this.data.weightValue,
      weightDate: today,
      feedingTypeIndex: 0,
      feedingAmount: '',
      feedingTime: time,
      feedingNotes: '',
      vaccineName: '',
      vaccineDate: today,
      vaccineNextDate: '',
      vaccineClinic: '',
      vaccineNotes: '',
      checkupDate: today,
      checkupClinic: '',
      checkupFindings: '',
      checkupRecommendations: '',
    });
  },

  closeSheet() {
    this.setData({ sheetVisible: false });
  },

  noop() {
    // 阻止点击面板时关闭遮罩
  },

  onFormInput(event: any) {
    const field = event.currentTarget.dataset.field;
    if (!field) {
      return;
    }
    this.setData({ [field]: event.detail.value });
  },

  onWeightDateChange(event: any) {
    this.setData({ weightDate: event.detail.value });
  },

  onFeedingTypeChange(event: any) {
    this.setData({ feedingTypeIndex: Number(event.detail.value) });
  },

  onFeedingTimeChange(event: any) {
    this.setData({ feedingTime: event.detail.value });
  },

  onVaccineDateChange(event: any) {
    this.setData({ vaccineDate: event.detail.value });
  },

  onVaccineNextDateChange(event: any) {
    this.setData({ vaccineNextDate: event.detail.value });
  },

  onCheckupDateChange(event: any) {
    this.setData({ checkupDate: event.detail.value });
  },

  async saveRecord() {
    const { currentPet, sheetType, saving } = this.data;
    if (!currentPet || saving) {
      return;
    }

    this.setData({ saving: true });
    try {
      if (sheetType === 'weight') {
        const weight = Number(this.data.weightValue);
        if (!weight || weight <= 0) {
          wx.showToast({ title: '请输入正确的体重', icon: 'none' });
          return;
        }
        await addWeightRecord({
          petId: currentPet.id,
          weightKg: weight,
          recordedAt: `${this.data.weightDate || todayString()} 00:00:00`,
        });
      } else if (sheetType === 'feeding') {
        const amount = this.data.feedingAmount
          ? Number(this.data.feedingAmount)
          : undefined;
        await addFeedingRecord({
          petId: currentPet.id,
          feedType:
            this.data.feedingTypeValues[this.data.feedingTypeIndex] ||
            'breakfast',
          amountG: amount,
          mealTime: `${todayString()} ${this.data.feedingTime || '08:00'}:00`,
          notes: this.data.feedingNotes.trim() || undefined,
        });
      } else if (sheetType === 'vaccine') {
        if (!this.data.vaccineName.trim()) {
          wx.showToast({ title: '请填写疫苗名称', icon: 'none' });
          return;
        }
        await addVaccineRecord({
          petId: currentPet.id,
          vaccineName: this.data.vaccineName.trim(),
          vaccinatedAt: this.data.vaccineDate || todayString(),
          nextDueAt: this.data.vaccineNextDate || undefined,
          vetClinic: this.data.vaccineClinic.trim() || undefined,
          notes: this.data.vaccineNotes.trim() || undefined,
        });
      } else if (sheetType === 'checkup') {
        await addCheckupRecord({
          petId: currentPet.id,
          checkupDate: this.data.checkupDate || todayString(),
          vetClinic: this.data.checkupClinic.trim() || undefined,
          findings: this.data.checkupFindings.trim() || undefined,
          recommendations:
            this.data.checkupRecommendations.trim() || undefined,
        });
      }

      this.setData({ sheetVisible: false });
      wx.showToast({ title: '保存成功', icon: 'success' });

      if (sheetType === 'weight') {
        const pets = await listPets().catch(() => []);
        const latestPet = (pets || []).find(
          (pet) => pet.id === currentPet.id,
        );
        if (latestPet) {
          this.setData({
            currentPet: latestPet,
            currentWeight: latestPet.weight
              ? String(latestPet.weight)
              : this.data.currentWeight,
          });
        }
      }
      await this.loadTab(this.data.activeTab, currentPet.id, true, true);
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },

  deleteRecord(event: any) {
    const { type, id } = event.currentTarget.dataset;
    const recordId = Number(id);
    if (!recordId) {
      return;
    }

    wx.showModal({
      title: '删除记录',
      content: '确定删除这条健康记录吗？',
      confirmText: '删除',
      confirmColor: '#e05b6f',
      success: async (result: any) => {
        if (!result.confirm) {
          return;
        }
        try {
          if (type === 'weight') {
            await deleteWeightRecord(recordId);
          } else if (type === 'feeding') {
            await deleteFeedingRecord(recordId);
          } else if (type === 'vaccine') {
            await deleteVaccineRecord(recordId);
          } else {
            await deleteCheckupRecord(recordId);
          }
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadTab(this.data.activeTab);
        } catch (error) {
          // request 已统一提示
        }
      },
    });
  },

  goCreatePet() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },
}, ["pet-placeholder.png", "health/health-weight-scale.png", "health/health-feeding-bowl.png", "health/health-vaccine-vial.png", "health/health-medical-kit.png"]));
