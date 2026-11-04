import React, { useEffect } from 'react';
import { Table, Button, Card, Space, Modal, message, Spin } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { useApp } from '@/hooks/useApp';
import * as petService from '@/services/petService';
import type { Pet } from '@/types/pet';

const PetListPage: React.FC = () => {
  const navigate = useNavigate();
  const { pets, setPets, currentPetId, setCurrentPetId, setCurrentPet, loading, setLoading } = useApp();

  useEffect(() => {
    const fetchPets = async () => {
      try {
        setLoading(true);
        const data = await petService.listPets();
        const petList = data || [];
        setPets(petList);
        if (petList.length > 0) {
          const selected = petList.find((pet) => pet.id === currentPetId) || petList[0];
          setCurrentPetId(selected.id);
          setCurrentPet(selected);
        }
      } catch (error: any) {
        message.error(error.message || '加载宠物列表失败');
      } finally {
        setLoading(false);
      }
    };

    fetchPets();
  }, []);

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '删除宠物',
      content: '确定要删除这只宠物吗？',
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await petService.deletePet(id);
          message.success('宠物已删除');
          const newPets = pets.filter((p) => p.id !== id);
          setPets(newPets);
          if (id === currentPetId) {
            const next = newPets[0] || null;
            setCurrentPetId(next ? next.id : null);
            setCurrentPet(next);
          }
        } catch (error: any) {
          message.error(error.message || '删除失败');
        }
      },
    });
  };

  const columns: ColumnsType<Pet> = [
    {
      title: '宠物名称',
      dataIndex: 'petName',
      key: 'petName',
    },
    {
      title: '类型',
      dataIndex: 'petType',
      key: 'petType',
    },
    {
      title: '品种',
      dataIndex: 'breed',
      key: 'breed',
    },
    {
      title: '性别',
      dataIndex: 'gender',
      key: 'gender',
      render: (gender: number) => {
        if (gender === 1) return '男';
        if (gender === 2) return '女';
        return '未知';
      },
    },
    {
      title: '体重',
      dataIndex: 'weight',
      key: 'weight',
      render: (weight: number) => `${weight} kg`,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => {
              setCurrentPetId(record.id);
              navigate(`/pet/${record.id}`);
            }}
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/pet/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="我的宠物"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/pet/create')}>
          添加宠物
        </Button>
      }
    >
      <Spin spinning={loading}>
        <Table
          columns={columns}
          dataSource={pets}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      </Spin>
    </Card>
  );
};

export default PetListPage;
