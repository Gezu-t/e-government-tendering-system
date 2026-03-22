import { useEffect, useState } from 'react';
import { Table, Tag, Card, Typography, Space, Button, Spin, Flex, Descriptions, Modal, Timeline, Progress } from 'antd';
import {
  ContainerOutlined, EyeOutlined, CheckCircleOutlined,
  ClockCircleOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons';
import { contractApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { Contract, ContractStatus, ContractMilestone } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const statusColors: Record<ContractStatus, string> = {
  DRAFT: 'default',
  PENDING_SIGNATURE: 'orange',
  ACTIVE: 'green',
  COMPLETED: 'blue',
  TERMINATED: 'red',
  CANCELLED: 'default',
};

const milestoneStatusIcon = (status: string) => {
  switch (status) {
    case 'COMPLETED': return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
    case 'OVERDUE': return <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />;
    default: return <ClockCircleOutlined style={{ color: '#1890ff' }} />;
  }
};

export default function ContractsPage() {
  const { userId, role } = useAuthStore();
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedContract, setSelectedContract] = useState<Contract | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  useEffect(() => {
    const fetchContracts = async () => {
      setLoading(true);
      try {
        if (role === 'TENDERER') {
          const { data } = await contractApi.getByBidder(userId!, { page: 0, size: 50 });
          setContracts(data.content ?? data);
        } else {
          const { data } = await contractApi.search({ page: 0, size: 50 });
          setContracts(data.content ?? data);
        }
      } catch {
        /* handled by interceptor */
      }
      setLoading(false);
    };
    if (userId) fetchContracts();
  }, [userId, role]);

  const viewDetail = (contract: Contract) => {
    setSelectedContract(contract);
    setDetailOpen(true);
  };

  const completedMilestones = (milestones: ContractMilestone[]) =>
    milestones?.filter((m) => m.status === 'COMPLETED').length ?? 0;

  const columns = [
    {
      title: 'Contract #',
      dataIndex: 'contractNumber',
      key: 'contractNumber',
      width: 160,
      render: (text: string, record: Contract) => (
        <a onClick={() => viewDetail(record)}>{text}</a>
      ),
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: 'Value',
      dataIndex: 'totalValue',
      key: 'totalValue',
      width: 130,
      render: (v: number) => `$${v?.toLocaleString() ?? '0'}`,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 160,
      render: (status: ContractStatus) => (
        <Tag color={statusColors[status]}>{status.replace(/_/g, ' ')}</Tag>
      ),
    },
    {
      title: 'Period',
      key: 'period',
      width: 200,
      render: (_: unknown, r: Contract) =>
        `${dayjs(r.startDate).format('MMM D, YYYY')} - ${dayjs(r.endDate).format('MMM D, YYYY')}`,
    },
    {
      title: 'Milestones',
      key: 'milestones',
      width: 120,
      render: (_: unknown, r: Contract) => {
        const total = r.milestones?.length ?? 0;
        const done = completedMilestones(r.milestones);
        return total > 0 ? <Text>{done}/{total}</Text> : <Text type="secondary">None</Text>;
      },
    },
    {
      title: 'Action',
      key: 'action',
      width: 90,
      render: (_: unknown, record: Contract) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => viewDetail(record)}>View</Button>
      ),
    },
  ];

  return (
    <Spin spinning={loading}>
      <Flex vertical gap={24} style={{ width: '100%' }}>
        <Space>
          <ContainerOutlined style={{ fontSize: 24 }} />
          <Title level={3} style={{ margin: 0 }}>Contracts</Title>
        </Space>

        <Card>
          <Table
            columns={columns}
            dataSource={contracts}
            rowKey="id"
            pagination={{ pageSize: 10, showSizeChanger: true }}
          />
        </Card>
      </Flex>

      <Modal
        title={selectedContract ? `Contract: ${selectedContract.contractNumber}` : 'Contract Details'}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={720}
      >
        {selectedContract && (
          <Flex vertical gap={20}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Title" span={2}>{selectedContract.title}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={statusColors[selectedContract.status]}>{selectedContract.status.replace(/_/g, ' ')}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Total Value">${selectedContract.totalValue?.toLocaleString()}</Descriptions.Item>
              <Descriptions.Item label="Start Date">{dayjs(selectedContract.startDate).format('MMM D, YYYY')}</Descriptions.Item>
              <Descriptions.Item label="End Date">{dayjs(selectedContract.endDate).format('MMM D, YYYY')}</Descriptions.Item>
              {selectedContract.description && (
                <Descriptions.Item label="Description" span={2}>{selectedContract.description}</Descriptions.Item>
              )}
            </Descriptions>

            {selectedContract.milestones?.length > 0 && (
              <Card title="Milestones" size="small">
                <Progress
                  percent={Math.round((completedMilestones(selectedContract.milestones) / selectedContract.milestones.length) * 100)}
                  style={{ marginBottom: 16 }}
                />
                <Timeline
                  items={selectedContract.milestones.map((m) => ({
                    dot: milestoneStatusIcon(m.status),
                    children: (
                      <div>
                        <Text strong>{m.title}</Text>
                        <br />
                        <Text type="secondary">Due: {dayjs(m.dueDate).format('MMM D, YYYY')}</Text>
                        {m.paymentAmount > 0 && <Text> &middot; ${m.paymentAmount.toLocaleString()}</Text>}
                        {m.completedDate && <><br /><Text type="success">Completed: {dayjs(m.completedDate).format('MMM D, YYYY')}</Text></>}
                      </div>
                    ),
                  }))}
                />
              </Card>
            )}

            {selectedContract.items?.length > 0 && (
              <Card title="Contract Items" size="small">
                <Table
                  dataSource={selectedContract.items}
                  rowKey="id"
                  pagination={false}
                  size="small"
                  columns={[
                    { title: 'Item', dataIndex: 'name', key: 'name' },
                    { title: 'Qty', dataIndex: 'quantity', key: 'qty', width: 70 },
                    { title: 'Unit', dataIndex: 'unit', key: 'unit', width: 70 },
                    { title: 'Unit Price', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, render: (v: number) => `$${v?.toLocaleString()}` },
                    { title: 'Total', dataIndex: 'totalPrice', key: 'total', width: 100, render: (v: number) => `$${v?.toLocaleString()}` },
                  ]}
                />
              </Card>
            )}
          </Flex>
        )}
      </Modal>
    </Spin>
  );
}
