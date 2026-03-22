import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, Typography, Spin, Tag, Space, Button } from 'antd';
import {
  FileTextOutlined, ThunderboltOutlined, ContainerOutlined, AuditOutlined,
  AlertOutlined, ReloadOutlined, BarChartOutlined, TeamOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { reportApi } from '../../api/services';
import apiClient from '../../api/client';

const { Title, Text } = Typography;

interface DashboardWidgets {
  activeTenders: number;
  pendingBids: number;
  activeContracts: number;
  auditAlertsToday: number;
  tendersPublishedThisMonth: number;
  bidsSubmittedThisMonth: number;
  contractsAwardedThisMonth: number;
  auditEntriesCreatedToday: number;
}

interface RecentAuditEntry {
  id: number;
  timestamp: string;
  action: string;
  entityType: string;
  username?: string;
  success: boolean;
}

export default function AdminDashboardPage() {
  const navigate = useNavigate();
  const [widgets, setWidgets] = useState<DashboardWidgets | null>(null);
  const [recentActivity, setRecentActivity] = useState<RecentAuditEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [widgetRes, activityRes] = await Promise.all([
        reportApi.getDashboardWidgets().catch(() => ({ data: null })),
        apiClient.get('/api/audit/search', { params: { page: 0, size: 8 } }).catch(() => ({ data: { content: [] } })),
      ]);
      if (widgetRes.data) setWidgets(widgetRes.data);
      setRecentActivity(activityRes.data?.content || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  if (loading && !widgets) {
    return <div style={{ textAlign: 'center', padding: 100 }}><Spin size="large" /></div>;
  }

  const statCards = [
    { title: 'Active Tenders', value: widgets?.activeTenders ?? 0, icon: <FileTextOutlined />, color: '#1890ff', path: '/admin/reports' },
    { title: 'Pending Bids', value: widgets?.pendingBids ?? 0, icon: <ThunderboltOutlined />, color: '#722ed1', path: '/admin/reports' },
    { title: 'Active Contracts', value: widgets?.activeContracts ?? 0, icon: <ContainerOutlined />, color: '#52c41a', path: '/contracts' },
    { title: 'Audit Alerts Today', value: widgets?.auditAlertsToday ?? 0, icon: <AlertOutlined />, color: widgets?.auditAlertsToday ? '#ff4d4f' : '#8c8c8c', path: '/admin/audit' },
  ];

  const monthlyStats = [
    { title: 'Tenders Published', value: widgets?.tendersPublishedThisMonth ?? 0, color: '#1890ff' },
    { title: 'Bids Submitted', value: widgets?.bidsSubmittedThisMonth ?? 0, color: '#722ed1' },
    { title: 'Contracts Awarded', value: widgets?.contractsAwardedThisMonth ?? 0, color: '#52c41a' },
    { title: 'Audit Entries Today', value: widgets?.auditEntriesCreatedToday ?? 0, color: '#faad14' },
  ];

  const activityColumns = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: (t: string) => new Date(t).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }),
    },
    { title: 'User', dataIndex: 'username', key: 'username', width: 120, render: (u: string) => u || 'system' },
    { title: 'Action', dataIndex: 'action', key: 'action', width: 150, render: (a: string) => <Tag color="blue">{a}</Tag> },
    { title: 'Entity', dataIndex: 'entityType', key: 'entityType', width: 120 },
    {
      title: 'Status',
      dataIndex: 'success',
      key: 'success',
      width: 80,
      render: (s: boolean) => s ? <Tag color="green">OK</Tag> : <Tag color="red">FAIL</Tag>,
    },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col><Title level={4} style={{ margin: 0 }}>Admin Dashboard</Title></Col>
        <Col>
          <Button icon={<ReloadOutlined />} onClick={fetchData} loading={loading}>Refresh</Button>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {statCards.map((s) => (
          <Col xs={24} sm={12} lg={6} key={s.title}>
            <Card hoverable onClick={() => navigate(s.path)} style={{ cursor: 'pointer' }}>
              <Statistic title={s.title} value={s.value} prefix={s.icon} styles={{ content: { color: s.color } }} />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={14}>
          <Card
            title="Recent Activity"
            extra={<Button type="link" onClick={() => navigate('/admin/audit')}>View All</Button>}
          >
            <Table
              dataSource={recentActivity}
              columns={activityColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="This Month" style={{ marginBottom: 16 }}>
            <Row gutter={[16, 16]}>
              {monthlyStats.map((s) => (
                <Col span={12} key={s.title}>
                  <Statistic title={s.title} value={s.value} styles={{ content: { color: s.color } }} />
                </Col>
              ))}
            </Row>
          </Card>
          <Card title="Quick Actions">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button block icon={<AuditOutlined />} onClick={() => navigate('/admin/audit')}>Audit Log</Button>
              <Button block icon={<BarChartOutlined />} onClick={() => navigate('/admin/reports')}>Reports</Button>
              <Button block icon={<TeamOutlined />} onClick={() => navigate('/admin/users')}>User Management</Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
