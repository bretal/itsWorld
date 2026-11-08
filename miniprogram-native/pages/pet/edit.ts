import { withStaticImages } from '../../utils/static-images';
import { requireLogin } from '../../utils/auth';
import { createPet, getPet, updatePet } from '../../services/pet';
import type { PetPayload } from '../../services/pet';
import { uploadImage } from '../../utils/upload';
import { todayString } from '../../utils/format';
import { resolveImage } from '../../utils/image';

Page(withStaticImages({
  data: {
    id: 0,
    isEdit: false,
    petName: '',
    petTypes: ['狗狗', '猫咪', '其他'],
    petTypeValues: ['dog', 'cat', 'other'],
    petTypeIndex: 0,
    genders: ['未知', '公', '母'],
    genderValues: [0, 1, 2],
    genderIndex: 0,
    breed: '',
    birthDate: '',
    today: '',
    weight: '',
    height: '',
    description: '',
    avatarUrl: '',
    avatarPreviewSrc: '',
    saving: false,
    uploading: false,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }

    const today = todayString();
    const id = Number(options?.id || 0);
    if (id) {
      wx.setNavigationBarTitle({ title: '编辑宠物档案' });
      this.setData({ id, isEdit: true, today });
      this.loadPet(id);
      return;
    }

    wx.setNavigationBarTitle({ title: '添加宠物' });
    this.setData({ today });
  },

  async loadPet(id: number) {
    try {
      const pet = await getPet(id);
      const petTypeIndex = Math.max(
        this.data.petTypeValues.indexOf(pet.petType || 'dog'),
        0,
      );
      const genderIndex = Math.max(
        this.data.genderValues.indexOf(pet.gender ?? 0),
        0,
      );

      this.setData({
        petName: pet.petName || '',
        petTypeIndex,
        genderIndex,
        breed: pet.breed || '',
        birthDate: pet.birthDate || '',
        weight:
          pet.weight === undefined || pet.weight === null
            ? ''
            : String(pet.weight),
        height:
          pet.height === undefined || pet.height === null
            ? ''
            : String(pet.height),
        description: pet.description || '',
        avatarUrl: pet.avatarUrl || '',
        avatarPreviewSrc: await resolveImage(pet.avatarUrl),
      });
    } catch (error) {
      // request 已统一提示
    }
  },

  onInput(event: any) {
    const field = event.currentTarget.dataset.field;
    if (!field) {
      return;
    }
    this.setData({ [field]: event.detail.value });
  },

  onPetTypeChange(event: any) {
    this.setData({ petTypeIndex: Number(event.detail.value) });
  },

  onGenderChange(event: any) {
    this.setData({ genderIndex: Number(event.detail.value) });
  },

  onBirthDateChange(event: any) {
    this.setData({ birthDate: event.detail.value });
  },

  async chooseAvatar() {
    if (this.data.uploading) {
      return;
    }

    try {
      const result = await new Promise<any>((resolve, reject) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject,
        });
      });

      const filePath = result.tempFiles?.[0]?.tempFilePath;
      if (!filePath) {
        return;
      }

      this.setData({ uploading: true });
      const url = await uploadImage(filePath, 'pet');
      const avatarPreviewSrc = await resolveImage(url);
      this.setData({ avatarUrl: url, avatarPreviewSrc });
    } catch (error) {
      // 用户取消或上传失败，无需额外提示
    } finally {
      this.setData({ uploading: false });
    }
  },

  async onSave() {
    const { petName, petTypeIndex, genderIndex, saving } = this.data;
    if (saving) {
      return;
    }
    if (!petName.trim()) {
      wx.showToast({ title: '请填写宠物名字', icon: 'none' });
      return;
    }

    const payload: PetPayload = {
      petName: petName.trim(),
      petType: this.data.petTypeValues[petTypeIndex],
      avatarUrl: this.data.avatarUrl || undefined,
      breed: this.data.breed.trim() || undefined,
      gender: this.data.genderValues[genderIndex],
      birthDate: this.data.birthDate || undefined,
      weight:
        this.data.weight === '' ? undefined : Number(this.data.weight),
      height:
        this.data.height === '' ? undefined : Number(this.data.height),
      description: this.data.description.trim() || undefined,
    };

    this.setData({ saving: true });
    try {
      if (this.data.isEdit) {
        await updatePet(this.data.id, payload);
        wx.showToast({ title: '保存成功', icon: 'success' });
      } else {
        const petId = await createPet(payload);
        const currentPetId = Number(wx.getStorageSync('currentPetId') || 0);
        if (!currentPetId && petId) {
          wx.setStorageSync('currentPetId', petId);
          try {
            const app = getApp<IAppOption>();
            app.globalData.currentPetId = petId;
          } catch (error) {
            // 忽略
          }
        }
        wx.showToast({ title: '添加成功', icon: 'success' });
      }

      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    } catch (error) {
      // request 已统一提示
    } finally {
      this.setData({ saving: false });
    }
  },
}, ["pet-placeholder.png"]));
