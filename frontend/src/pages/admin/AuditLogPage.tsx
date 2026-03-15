import { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, Tag, Typography, Space, Select, DatePicker, Button,
  Row, Col, Statistic, message, Spin,
} from 'antd';
import {
  AuditOutlined, CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined,
} from '@ant-design/icons';
import apiClient from '../../api/client';
import { reportApi } from '../../api/services';
import type { Page } from '../../types';
import dayjs, { Dayjs } from 'dayjs';

const { Title } = Typography;
const { RangePicker } = DatePicker;

interface AuditLogEntry {
  id: number;
  timestamp: string;
  action: string;
  entityType: string;
  entityId: string;
  userId: number;
  username?: string;
  success: boolean;
  details?: string;
}

interface AuditSummary {
  totalEntries?: number;
  successCount?: number;
  failureCount?: number;
}

const ACTION_OPTIONS = [
  'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT',
  'SUBMIT', 'APPROVE', 'REJECT', 'PUBLISH', 'CLOSE',
];

const ENTITY_TYPE_OPTIONS = [
  'TENDER', 'BID', 'EVALUATION', 'CONTRACT', 'USER',
  'NOTIFICATION', 'DOCUMENT', 'ORGANIZATION',
];

export default function AuditLogPage() {
  const [loading, setLoading] = useState(false);
  const [entries, setEntries] = useState<AuditLogEntry[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [summary, setSummary] = useState<AuditSummary>({});
  const [summaryLoading, setSummaryLoading] = useState(false);

  // Filters
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [actionFilter, setActionFilter] = useState<string | undefined>();
  const [entityTypeFilter, setEntityTypeFilter] = useState<string | undefined>();

  const fetchAuditLogs = useCallback(async (page = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = {
        page: page - 1,
        size: pageSize,
      };
      if (dateRange?.[0]) params.from = dateRange[0].format('YYYY-MM-DD');
      if (dateRange?.[1]) params.to = dateRange[1].format('YYYY-MM-DD');
      if (actionFilter) params.action = actionFilter;
      if (entityTypeFilter) params.entityType = entityTypeFilter;

      const res = await apiClient.get<Page<AuditLogEntry>>('/api/audit/search', { params });
      const data = res.data;
      setEntries(data.content);
      setPagination({
        current: data.number + 1,
        pageSize: data.size,
        total: data.totalElements,
      });
    } catch {
      message.error('Failed to fetch audit logs');
    } finally {
      setLoading(false);
    }
  }, [dateRange, actionFilter, entityTypeFilter]);

  const fetchSummary = useCallback(async () => {
    setSummaryLoading(true);
    try {
      const from = dateRange?.[0]?.format('YYYY-MM-DD');
      const to = dateRange?.[1]?.format('YYYY-MM-DD');
      const res = await reportApi.getAuditActivity(from, to);
      setSummary(res.data ?? {});
    } catch {
      // Summary is optional, do not block the page
    } finally {
      setSummaryLoading(false);
    }
  }, [dateRange]);

  useEffect(() => {
    fetchAuditLogs();
    fetchSummary();
  }, [fetchAuditLogs, fetchSummary]);

  const handleSearch = () => {
    fetchAuditLogs(1, pagination.pageSize);
    fetchSummary();
  };

  const handleReset = () => {
    setDateRange(null);
    setActionFilter(undefined);
    setEntityTypeFilter(undefined);
  };

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 120,
      render: (val: string) => <Tag color="blue">{val}</Tag>,
    },
    {
      title: 'Entity Type',
      dataIndex: 'entityType',
      key: 'entityType',
      width: 130,
      render: (val: string) => <Tag>{val}</Tag>,
    },
    {
      title: 'Entity ID',
      dataIndex: 'entityId',
      key: 'entityId',
      width: 100,
    },
    {
      title: 'User',
      key: 'user',
      width: 150,
      render: (_: unknown, record: AuditLogEntry) => record.username ?? `User #${record.userId}`,
    },
    {
      title: 'Result',
      dataIndex: 'success',
      key: 'success',
      width: 100,
      align: 'center' as const,
      render: (val: boolean) =>
        val ? (
          <Tag icon={<CheckCircleOutlined />} color="success">Success</Tag>
        ) : (
          <Tag icon={<CloseCircleOutlined />} color="error">Failure</Tag>
        ),
    },
    {
      title: 'Details',
      dataIndex: 'details',
      key: 'details',
      ellipsis: true,
    },
  ];

  return (
    <div style={{ padding: '24px 16px' }}>
      <Title level={3}>Audit Log</Title>

      {/* Summary Stats */}
      <Spin spinning={summaryLoading}>
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Total Entries"
                value={summary.totalEntries ?? pagination.total}
                prefix={<AuditOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Successful"
                value={summary.successCount ?? '-'}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Failures"
                value={summary.failureCount ?? '-'}
                prefix={<CloseCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
        </Row>
      </Spin>

      {/* Filters */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <RangePicker
            value={dateRange as [Dayjs, Dayjs] | null}
            onChange={(vals) => setDateRange(vals as [Dayjs | null, Dayjs | null] | null)}
            allowClear
          />
          <Select
            placeholder="Action Type"
            value={actionFilter}
            onChange={setActionFilter}
            allowClear
            style={{ width: 160 }}
            options={ACTION_OPTIONS.map((a) => ({ label: a, value: a }))}
          />
          <Select
            placeholder="Entity Type"
            value={entityTypeFilter}
            onChange={setEntityTypeFilter}
            allowClear
            style={{ width: 160 }}
            options={ENTITY_TYPE_OPTIONS.map((e) => ({ label: e, value: e }))}
          />
          <Button type="primary" onClick={handleSearch}>
            Search
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            Reset
          </Button>
        </Space>
      </Card>

      {/* Table */}
      <Card>
        <Table
          dataSource={entries}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `Total ${total} entries`,
            onChange: (page, pageSize) => fetchAuditLogs(page, pageSize),
          }}
          scroll={{ x: 900 }}
        />
      </Card>
    </div>
  );
}
