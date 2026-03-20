import { useState, useEffect, useCallback } from 'react';
import {
  Card, Typography, Row, Col, Statistic, DatePicker, Button, Space,
  Descriptions, Progress, Spin, Divider, Tag, Empty, Tabs, Flex,
} from 'antd';
import {
  BarChartOutlined, AuditOutlined, AppstoreOutlined, ThunderboltOutlined,
  ReloadOutlined, FileTextOutlined, TeamOutlined, ContainerOutlined,
  AlertOutlined,
} from '@ant-design/icons';
import { reportApi } from '../../api/services';
import { useMessage } from '../../hooks/useMessage';
import type { Dayjs } from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const ACTION_COLORS: Record<string, string> = {
  CREATE: '#52c41a', UPDATE: '#1890ff', DELETE: '#ff4d4f',
  LOGIN: '#722ed1', SUBMIT: '#faad14', APPROVE: '#13c2c2',
  REJECT: '#f5222d', PUBLISH: '#2f54eb', CLOSE: '#8c8c8c',
};
const MODULE_COLORS = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'];

// ── shared date-filter bar ─────────────────────────────────────────────────
function DateFilter({
  dateRange, setDateRange, onApply,
}: {
  dateRange: [Dayjs | null, Dayjs | null] | null;
  setDateRange: (v: [Dayjs | null, Dayjs | null] | null) => void;
  onApply: () => void;
}) {
  return (
    <Card style={{ marginBottom: 24 }}>
      <Space wrap>
        <Text strong>Filter Period:</Text>
        <RangePicker
          value={dateRange as [Dayjs, Dayjs] | null}
          onChange={(vals) => setDateRange(vals as [Dayjs | null, Dayjs | null] | null)}
          allowClear
        />
        <Button type="primary" onClick={onApply}>Apply</Button>
        <Button icon={<ReloadOutlined />} onClick={() => setDateRange(null)}>Reset</Button>
      </Space>
    </Card>
  );
}

// ── status bar list ────────────────────────────────────────────────────────
function StatusBars({ data, colors }: { data: [string, number][]; colors: string[] }) {
  const max = data.length > 0 ? Math.max(...data.map(([, c]) => c)) : 1;
  return data.length > 0 ? (
    <Flex vertical gap={16}>
      {data.map(([label, count], idx) => (
        <div key={label}>
          <Space style={{ marginBottom: 4 }}>
            <Tag color={colors[idx % colors.length]}>{label}</Tag>
            <Text type="secondary">{count} entries</Text>
          </Space>
          <Progress
            percent={Math.round((count / max) * 100)}
            strokeColor={colors[idx % colors.length]}
            format={() => count.toLocaleString()}
          />
        </div>
      ))}
    </Flex>
  ) : <Empty description="No data available" />;
}

// ── Procurement / Audit Activity tab ──────────────────────────────────────
function ProcurementTab() {
  const message = useMessage();
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<Record<string, unknown>>({});
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const from = dateRange?.[0]?.format('YYYY-MM-DD');
      const to = dateRange?.[1]?.format('YYYY-MM-DD');
      const res = await reportApi.getProcurementSummary(from, to);
      setSummary((res.data ?? {}) as Record<string, unknown>);
    } catch {
      message.error('Failed to load procurement summary');
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => { fetch(); }, [fetch]);

  const actionCounts = Object.entries((summary.byAction as Record<string, number>) ?? {})
    .sort(([, a], [, b]) => b - a).slice(0, 5) as [string, number][];
  const moduleCounts = Object.entries((summary.byModule as Record<string, number>) ?? {})
    .sort(([, a], [, b]) => b - a) as [string, number][];

  return (
    <Spin spinning={loading}>
      <DateFilter dateRange={dateRange} setDateRange={setDateRange} onApply={fetch} />
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Total Audit Entries" value={(summary.totalEntries as number) ?? 0}
            prefix={<AuditOutlined />} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Action Types" value={actionCounts.length}
            prefix={<ThunderboltOutlined />} valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Modules" value={moduleCounts.length}
            prefix={<AppstoreOutlined />} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Top Action" value={actionCounts[0]?.[0] ?? '-'}
            prefix={<BarChartOutlined />} valueStyle={{ color: '#faad14', fontSize: 20 }} /></Card>
        </Col>
      </Row>
      <Card title="Top 5 Actions by Count" style={{ marginBottom: 24 }}>
        <StatusBars
          data={actionCounts}
          colors={actionCounts.map(([k]) => ACTION_COLORS[k] ?? '#8c8c8c')}
        />
      </Card>
      <Card title="Entries by Module">
        <StatusBars data={moduleCounts} colors={MODULE_COLORS} />
      </Card>
    </Spin>
  );
}

// ── Tender Status tab ──────────────────────────────────────────────────────
function TenderStatusTab() {
  const message = useMessage();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Record<string, unknown>>({});
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const from = dateRange?.[0]?.format('YYYY-MM-DD');
      const to = dateRange?.[1]?.format('YYYY-MM-DD');
      const res = await reportApi.getTenderStatusReport(from, to);
      setData((res.data ?? {}) as Record<string, unknown>);
    } catch {
      message.error('Failed to load tender status report');
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => { fetch(); }, [fetch]);

  const statusEntries = Object.entries((data.byStatus as Record<string, number>) ?? {})
    .sort(([, a], [, b]) => b - a) as [string, number][];

  return (
    <Spin spinning={loading}>
      <DateFilter dateRange={dateRange} setDateRange={setDateRange} onApply={fetch} />
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Total Tenders" value={(data.totalDistinctTenders as number) ?? 0}
            prefix={<FileTextOutlined />} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Published This Period" value={(data.publishedThisPeriod as number) ?? 0}
            prefix={<ThunderboltOutlined />} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Closed This Period" value={(data.closedThisPeriod as number) ?? 0}
            prefix={<AuditOutlined />} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Awarded This Period" value={(data.awardedThisPeriod as number) ?? 0}
            prefix={<BarChartOutlined />} valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
      </Row>
      <Card title="Tenders by Status" style={{ marginBottom: 24 }}>
        <StatusBars data={statusEntries} colors={MODULE_COLORS} />
      </Card>
      <Card title="Period Details">
        <Descriptions bordered column={{ xs: 1, sm: 2 }}>
          <Descriptions.Item label="Reporting Period">{(data.period as string) ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Total Tenders">{((data.totalDistinctTenders as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Published">{((data.publishedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Closed">{((data.closedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Awarded">{((data.awardedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Amended">{((data.amendedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Spin>
  );
}

// ── Bid Statistics tab ─────────────────────────────────────────────────────
function BidStatisticsTab() {
  const message = useMessage();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Record<string, unknown>>({});
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const from = dateRange?.[0]?.format('YYYY-MM-DD');
      const to = dateRange?.[1]?.format('YYYY-MM-DD');
      const res = await reportApi.getBidStatistics(from, to);
      setData((res.data ?? {}) as Record<string, unknown>);
    } catch {
      message.error('Failed to load bid statistics');
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => { fetch(); }, [fetch]);

  const statusEntries = Object.entries((data.byStatus as Record<string, number>) ?? {})
    .sort(([, a], [, b]) => b - a) as [string, number][];

  return (
    <Spin spinning={loading}>
      <DateFilter dateRange={dateRange} setDateRange={setDateRange} onApply={fetch} />
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Total Bids" value={(data.totalBids as number) ?? 0}
            prefix={<TeamOutlined />} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Submitted This Period" value={(data.submittedThisPeriod as number) ?? 0}
            prefix={<ThunderboltOutlined />} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Avg Bids / Tender" value={((data.averageBidsPerTender as number) ?? 0).toFixed(1)}
            prefix={<ContainerOutlined />} valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card><Statistic title="Flagged Bids" value={(data.flaggedBids as number) ?? 0}
            prefix={<AlertOutlined />} valueStyle={{ color: '#ff4d4f' }} /></Card>
        </Col>
      </Row>
      <Card title="Bids by Status" style={{ marginBottom: 24 }}>
        <StatusBars data={statusEntries} colors={MODULE_COLORS} />
      </Card>
      <Card title="Period Details">
        <Descriptions bordered column={{ xs: 1, sm: 2 }}>
          <Descriptions.Item label="Reporting Period">{(data.period as string) ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Total Bids">{((data.totalBids as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Submitted">{((data.submittedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Evaluated">{((data.evaluatedThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Withdrawn">{((data.withdrawnThisPeriod as number) ?? 0).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Flagged">{((data.flaggedBids as number) ?? 0).toLocaleString()}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Spin>
  );
}

// ── Dashboard Widgets tab ──────────────────────────────────────────────────
function DashboardWidgetsTab() {
  const message = useMessage();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Record<string, number>>({});

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reportApi.getDashboardWidgets();
      setData((res.data ?? {}) as Record<string, number>);
    } catch {
      message.error('Failed to load dashboard widgets');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetch(); }, [fetch]);

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<ReloadOutlined />} onClick={fetch}>Refresh</Button>
      </div>
      <Row gutter={[16, 16]}>
        {[
          { key: 'activeTenders', label: 'Active Tenders', icon: <FileTextOutlined />, color: '#1890ff' },
          { key: 'pendingBids', label: 'Pending Bids', icon: <TeamOutlined />, color: '#faad14' },
          { key: 'activeContracts', label: 'Active Contracts', icon: <ContainerOutlined />, color: '#52c41a' },
          { key: 'auditAlertsToday', label: 'Audit Alerts Today', icon: <AlertOutlined />, color: '#ff4d4f' },
          { key: 'tendersPublishedThisMonth', label: 'Tenders Published (Month)', icon: <BarChartOutlined />, color: '#722ed1' },
          { key: 'bidsSubmittedThisMonth', label: 'Bids Submitted (Month)', icon: <ThunderboltOutlined />, color: '#13c2c2' },
          { key: 'contractsAwardedThisMonth', label: 'Contracts Awarded (Month)', icon: <AppstoreOutlined />, color: '#eb2f96' },
          { key: 'auditEntriesCreatedToday', label: 'Audit Entries Today', icon: <AuditOutlined />, color: '#fa8c16' },
        ].map(({ key, label, icon, color }) => (
          <Col key={key} xs={24} sm={12} md={6}>
            <Card>
              <Statistic title={label} value={data[key] ?? 0}
                prefix={icon} valueStyle={{ color }} />
            </Card>
          </Col>
        ))}
      </Row>
      <Divider />
      <Card title="All Widget Values">
        <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }}>
          {Object.entries(data).map(([k, v]) => (
            <Descriptions.Item key={k} label={k}>{v.toLocaleString()}</Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
    </Spin>
  );
}

// ── Page ───────────────────────────────────────────────────────────────────
export default function ReportsPage() {
  return (
    <div style={{ padding: '24px 16px' }}>
      <Title level={3}>Procurement Reports</Title>
      <Tabs
        defaultActiveKey="dashboard"
        items={[
          { key: 'dashboard', label: 'Dashboard Widgets', children: <DashboardWidgetsTab /> },
          { key: 'procurement', label: 'Audit Activity', children: <ProcurementTab /> },
          { key: 'tenders', label: 'Tender Status', children: <TenderStatusTab /> },
          { key: 'bids', label: 'Bid Statistics', children: <BidStatisticsTab /> },
        ]}
      />
    </div>
  );
}
