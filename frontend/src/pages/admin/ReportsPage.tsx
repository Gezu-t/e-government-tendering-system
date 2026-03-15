import { useState, useEffect, useCallback } from 'react';
import {
  Card, Typography, Row, Col, Statistic, DatePicker, Button, Space,
  Descriptions, Progress, Spin, message, Divider, Tag, Empty,
} from 'antd';
import {
  BarChartOutlined, AuditOutlined, AppstoreOutlined, ThunderboltOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { reportApi } from '../../api/services';
import type { Dayjs } from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

interface ActionCount {
  action: string;
  count: number;
}

interface ModuleCount {
  module: string;
  count: number;
}

interface ProcurementSummary {
  totalEntries?: number;
  actionCounts?: ActionCount[];
  moduleCounts?: ModuleCount[];
}

const ACTION_COLORS: Record<string, string> = {
  CREATE: '#52c41a',
  UPDATE: '#1890ff',
  DELETE: '#ff4d4f',
  LOGIN: '#722ed1',
  SUBMIT: '#faad14',
  APPROVE: '#13c2c2',
  REJECT: '#f5222d',
  PUBLISH: '#2f54eb',
  CLOSE: '#8c8c8c',
};

const MODULE_COLORS = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'];

export default function ReportsPage() {
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<ProcurementSummary>({});
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const fetchSummary = useCallback(async () => {
    setLoading(true);
    try {
      const from = dateRange?.[0]?.format('YYYY-MM-DD');
      const to = dateRange?.[1]?.format('YYYY-MM-DD');
      const res = await reportApi.getProcurementSummary(from, to);
      setSummary(res.data ?? {});
    } catch {
      message.error('Failed to load procurement summary');
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  const handleReset = () => {
    setDateRange(null);
  };

  const topActions = (summary.actionCounts ?? [])
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  const maxActionCount = topActions.length > 0 ? topActions[0].count : 1;

  const modules = summary.moduleCounts ?? [];
  const maxModuleCount = modules.length > 0 ? Math.max(...modules.map((m) => m.count)) : 1;

  return (
    <div style={{ padding: '24px 16px' }}>
      <Title level={3}>Procurement Reports</Title>

      {/* Date Filter */}
      <Card style={{ marginBottom: 24 }}>
        <Space wrap size="middle">
          <Text strong>Filter Period:</Text>
          <RangePicker
            value={dateRange as [Dayjs, Dayjs] | null}
            onChange={(vals) => setDateRange(vals as [Dayjs | null, Dayjs | null] | null)}
            allowClear
          />
          <Button type="primary" onClick={fetchSummary}>
            Apply
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            Reset
          </Button>
        </Space>
      </Card>

      <Spin spinning={loading}>
        {/* Summary Cards */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="Total Audit Entries"
                value={summary.totalEntries ?? 0}
                prefix={<AuditOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="Action Types"
                value={(summary.actionCounts ?? []).length}
                prefix={<ThunderboltOutlined />}
                valueStyle={{ color: '#722ed1' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="Modules"
                value={modules.length}
                prefix={<AppstoreOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card>
              <Statistic
                title="Top Action"
                value={topActions[0]?.action ?? '-'}
                prefix={<BarChartOutlined />}
                valueStyle={{ color: '#faad14', fontSize: 20 }}
              />
            </Card>
          </Col>
        </Row>

        {/* Top 5 Actions Bar Chart (using Progress bars) */}
        <Card
          title="Top 5 Actions by Count"
          style={{ marginBottom: 24 }}
        >
          {topActions.length > 0 ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {topActions.map((item) => (
                <div key={item.action}>
                  <Space style={{ marginBottom: 4 }}>
                    <Tag color={ACTION_COLORS[item.action] ?? '#8c8c8c'}>{item.action}</Tag>
                    <Text type="secondary">{item.count} entries</Text>
                  </Space>
                  <Progress
                    percent={Math.round((item.count / maxActionCount) * 100)}
                    strokeColor={ACTION_COLORS[item.action] ?? '#1890ff'}
                    format={() => item.count.toLocaleString()}
                  />
                </div>
              ))}
            </Space>
          ) : (
            <Empty description="No action data available" />
          )}
        </Card>

        {/* Entries by Module */}
        <Card
          title="Entries by Module"
          style={{ marginBottom: 24 }}
        >
          {modules.length > 0 ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {modules.map((item, idx) => (
                <div key={item.module}>
                  <Space style={{ marginBottom: 4 }}>
                    <Tag color={MODULE_COLORS[idx % MODULE_COLORS.length]}>{item.module}</Tag>
                    <Text type="secondary">{item.count} entries</Text>
                  </Space>
                  <Progress
                    percent={Math.round((item.count / maxModuleCount) * 100)}
                    strokeColor={MODULE_COLORS[idx % MODULE_COLORS.length]}
                    format={() => item.count.toLocaleString()}
                  />
                </div>
              ))}
            </Space>
          ) : (
            <Empty description="No module data available" />
          )}
        </Card>

        {/* Detailed Breakdown */}
        <Card title="Summary Details">
          <Descriptions bordered column={{ xs: 1, sm: 2 }}>
            <Descriptions.Item label="Total Audit Entries">
              {(summary.totalEntries ?? 0).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="Reporting Period">
              {dateRange?.[0] && dateRange?.[1]
                ? `${dateRange[0].format('YYYY-MM-DD')} to ${dateRange[1].format('YYYY-MM-DD')}`
                : 'All time'}
            </Descriptions.Item>
            <Descriptions.Item label="Number of Action Types">
              {(summary.actionCounts ?? []).length}
            </Descriptions.Item>
            <Descriptions.Item label="Number of Modules">
              {modules.length}
            </Descriptions.Item>
          </Descriptions>

          {topActions.length > 0 && (
            <>
              <Divider orientationMargin={0}>Action Breakdown</Divider>
              <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }} size="small">
                {(summary.actionCounts ?? []).map((item) => (
                  <Descriptions.Item key={item.action} label={item.action}>
                    {item.count.toLocaleString()}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            </>
          )}
        </Card>
      </Spin>
    </div>
  );
}
