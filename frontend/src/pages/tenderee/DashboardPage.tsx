import { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Typography, Space, Button, Spin } from 'antd';
import {
  FileTextOutlined,
  SendOutlined,
  AuditOutlined,
  TrophyOutlined,
  EyeOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { tenderApi } from '../../api/services';
import type { Tender, TenderStatus } from '../../types';
import dayjs from 'dayjs';

const { Title } = Typography;

const statusColors: Record<TenderStatus, string> = {
  DRAFT: 'default',
  PUBLISHED: 'green',
  AMENDED: 'purple',
  CLOSED: 'red',
  EVALUATION_IN_PROGRESS: 'blue',
  EVALUATED: 'cyan',
  AWARDED: 'gold',
  CANCELLED: 'default',
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const [tenders, setTenders] = useState<Tender[]>([]);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState({
    total: 0,
    published: 0,
    underEvaluation: 0,
    awarded: 0,
  });

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const { data } = await tenderApi.getMyTenders({ page: 0, size: 100 });
        const all = data.content;
        setTenders(all.slice(0, 5));
        setStats({
          total: data.totalElements,
          published: all.filter((t) => t.status === 'PUBLISHED' || t.status === 'AMENDED').length,
          underEvaluation: all.filter(
            (t) => t.status === 'EVALUATION_IN_PROGRESS' || t.status === 'EVALUATED'
          ).length,
          awarded: all.filter((t) => t.status === 'AWARDED').length,
        });
      } catch {
        /* handled by interceptor */
      }
      setLoading(false);
    };
    fetchData();
  }, []);

  const columns = [
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      render: (text: string, record: Tender) => (
        <a onClick={() => navigate(`/tenders/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 160,
      render: (status: TenderStatus) => (
        <Tag color={statusColors[status]}>{status.replace(/_/g, ' ')}</Tag>
      ),
    },
    {
      title: 'Deadline',
      dataIndex: 'submissionDeadline',
      key: 'deadline',
      width: 170,
      render: (d: string) => dayjs(d).format('MMM D, YYYY HH:mm'),
    },
    {
      title: 'Action',
      key: 'action',
      width: 90,
      render: (_: unknown, record: Tender) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/tenders/${record.id}`)}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <Spin spinning={loading}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Title level={3} style={{ margin: 0 }}>
            Tenderee Dashboard
          </Title>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/tenders/create')}
          >
            Create Tender
          </Button>
        </Space>

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable>
              <Statistic
                title="Total Tenders"
                value={stats.total}
                prefix={<FileTextOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable>
              <Statistic
                title="Published"
                value={stats.published}
                prefix={<SendOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable>
              <Statistic
                title="Under Evaluation"
                value={stats.underEvaluation}
                prefix={<AuditOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card hoverable>
              <Statistic
                title="Awarded"
                value={stats.awarded}
                prefix={<TrophyOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
        </Row>

        <Card title="Recent Tenders">
          <Table
            columns={columns}
            dataSource={tenders}
            rowKey="id"
            pagination={false}
            size="middle"
          />
        </Card>
      </Space>
    </Spin>
  );
}
