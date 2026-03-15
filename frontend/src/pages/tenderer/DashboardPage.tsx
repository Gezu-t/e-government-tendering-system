import { useEffect, useState, useMemo } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Typography, Spin, Alert } from 'antd';
import {
  FileTextOutlined,
  SendOutlined,
  HourglassOutlined,
  TrophyOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { bidApi, userApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { Bid, BidStatus, User } from '../../types';
import dayjs from 'dayjs';

const { Title } = Typography;

const bidStatusColors: Record<BidStatus, string> = {
  DRAFT: 'default',
  SUBMITTED: 'blue',
  UNDER_EVALUATION: 'orange',
  ACCEPTED: 'cyan',
  REJECTED: 'red',
  EVALUATED: 'purple',
  AWARDED: 'green',
  CONTRACTED: 'gold',
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const { userId } = useAuthStore();

  const [bids, setBids] = useState<Bid[]>([]);
  const [loading, setLoading] = useState(true);
  const [qualified, setQualified] = useState<boolean | null>(null);
  const [qualLoading, setQualLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const { data } = await bidApi.getMyBids({ page: 0, size: 100 });
        setBids(data.content);
      } catch {
        /* handled by interceptor */
      }
      setLoading(false);
    };

    const fetchQualification = async () => {
      setQualLoading(true);
      try {
        if (userId) {
          const { data: user } = await userApi.getById(userId) as { data: User };
          const orgId = user.organizations?.[0]?.organizationId;
          if (orgId) {
            const { data: isQualified } = await userApi.isQualified(orgId);
            setQualified(isQualified);
          } else {
            setQualified(false);
          }
        }
      } catch {
        setQualified(null);
      }
      setQualLoading(false);
    };

    fetchData();
    fetchQualification();
  }, [userId]);

  const stats = useMemo(() => {
    const total = bids.length;
    const submitted = bids.filter((b) => b.status === 'SUBMITTED').length;
    const underEvaluation = bids.filter((b) => b.status === 'UNDER_EVALUATION').length;
    const awarded = bids.filter((b) => b.status === 'AWARDED').length;
    return { total, submitted, underEvaluation, awarded };
  }, [bids]);

  const recentBids = useMemo(() => {
    return [...bids]
      .sort((a, b) => dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf())
      .slice(0, 5);
  }, [bids]);

  const columns = [
    {
      title: 'Bid ID',
      dataIndex: 'id',
      key: 'id',
      render: (id: number) => <a onClick={() => navigate(`/tenderer/bids/${id}`)}>#{id}</a>,
    },
    {
      title: 'Tender ID',
      dataIndex: 'tenderId',
      key: 'tenderId',
    },
    {
      title: 'Total Price',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      render: (price: number) =>
        new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: BidStatus) => (
        <Tag color={bidStatusColors[status]}>{status.replace(/_/g, ' ')}</Tag>
      ),
    },
    {
      title: 'Submitted',
      dataIndex: 'submissionTime',
      key: 'submissionTime',
      render: (d: string | undefined) => (d ? dayjs(d).format('MMM D, YYYY HH:mm') : '-'),
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (d: string) => dayjs(d).format('MMM D, YYYY'),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Tenderer Dashboard</Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Bids"
              value={stats.total}
              prefix={<FileTextOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Submitted"
              value={stats.submitted}
              prefix={<SendOutlined />}
              valueStyle={{ color: '#1890ff' }}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Under Evaluation"
              value={stats.underEvaluation}
              prefix={<HourglassOutlined />}
              valueStyle={{ color: '#fa8c16' }}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Awarded"
              value={stats.awarded}
              prefix={<TrophyOutlined />}
              valueStyle={{ color: '#52c41a' }}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card title="Qualification Status">
            {qualLoading ? (
              <Spin />
            ) : qualified === true ? (
              <Alert
                message="Qualified"
                description="Your organization is pre-qualified to submit bids."
                type="success"
                showIcon
                icon={<SafetyCertificateOutlined />}
              />
            ) : qualified === false ? (
              <Alert
                message="Not Qualified"
                description="Your organization has not yet been pre-qualified. Please complete the qualification process."
                type="warning"
                showIcon
                action={
                  <a onClick={() => navigate('/tenderer/qualification')}>Apply Now</a>
                }
              />
            ) : (
              <Alert
                message="Status Unknown"
                description="Unable to determine qualification status."
                type="info"
                showIcon
              />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card title="Recent Bids">
            <Table
              columns={columns}
              dataSource={recentBids}
              rowKey="id"
              loading={loading}
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
