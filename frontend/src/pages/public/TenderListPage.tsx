import { useCallback, useEffect, useRef, useState } from 'react';
import { Table, Tag, Input, Select, Card, Typography, Space, Button, Flex } from 'antd';
import { SearchOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { tenderApi } from '../../api/services';
import type { Tender, TenderStatus, TenderType } from '../../types';
import dayjs from 'dayjs';

const { Title } = Typography;

const statusColors: Record<TenderStatus, string> = {
  DRAFT: 'default', PUBLISHED: 'green', AMENDED: 'purple', CLOSED: 'red',
  EVALUATION_IN_PROGRESS: 'blue', EVALUATED: 'cyan', AWARDED: 'gold', CANCELLED: 'default',
};

export default function TenderListPage() {
  const navigate = useNavigate();
  const [tenders, setTenders] = useState<Tender[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<TenderStatus | undefined>();

  // Keep a ref so fetchTenders can always read the latest search value without
  // adding it to the useCallback deps (which would auto-fetch on every keystroke).
  const searchRef = useRef(search);
  searchRef.current = search;

  const fetchTenders = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await tenderApi.getAll({ title: searchRef.current || undefined, status: statusFilter, page, size: 10 });
      setTenders(data.content);
      setTotal(data.totalElements);
    } catch { /* handled by interceptor */ }
    setLoading(false);
  }, [page, statusFilter]);

  useEffect(() => { fetchTenders(); }, [fetchTenders]);

  const columns = [
    {
      title: 'Title', dataIndex: 'title', key: 'title',
      render: (text: string, record: Tender) => (
        <a onClick={() => navigate(`/tenders/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Type', dataIndex: 'type', key: 'type',
      render: (type: TenderType) => <Tag>{type}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (status: TenderStatus) => <Tag color={statusColors[status]}>{status}</Tag>,
    },
    {
      title: 'Deadline', dataIndex: 'submissionDeadline', key: 'deadline',
      render: (d: string) => dayjs(d).format('MMM D, YYYY HH:mm'),
    },
    {
      title: 'Created', dataIndex: 'createdAt', key: 'createdAt',
      render: (d: string) => dayjs(d).format('MMM D, YYYY'),
    },
    {
      title: 'Action', key: 'action',
      render: (_: unknown, record: Tender) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => navigate(`/tenders/${record.id}`)}>
          View
        </Button>
      ),
    },
  ];

  return (
    <Card>
      <Flex vertical gap={24} style={{ width: '100%' }}>
        <Title level={4}>Published Tenders</Title>
        <Space>
          <Input
            placeholder="Search by title..."
            prefix={<SearchOutlined />}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onPressEnter={fetchTenders}
            style={{ width: 300 }}
          />
          <Select
            placeholder="Filter by status"
            allowClear
            style={{ width: 200 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'PUBLISHED', label: 'Published' },
              { value: 'AMENDED', label: 'Amended' },
              { value: 'CLOSED', label: 'Closed' },
              { value: 'AWARDED', label: 'Awarded' },
            ]}
          />
          <Button type="primary" onClick={fetchTenders}>Search</Button>
        </Space>
        <Table
          columns={columns}
          dataSource={tenders}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page + 1,
            total,
            pageSize: 10,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Flex>
    </Card>
  );
}
