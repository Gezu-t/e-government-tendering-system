import { useState, useEffect } from 'react';
import {
  Card, Table, Tag, Typography, Spin, message, Button, Row, Col, Statistic,
} from 'antd';
import {
  FileSearchOutlined, CheckCircleOutlined, UnorderedListOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { tenderApi, evaluationApi, bidApi } from '../../api/services';
import type { Tender, Evaluation, Page, Bid } from '../../types';

const { Title } = Typography;

interface AssignedTender extends Tender {
  evaluationCount: number;
  pendingBids: number;
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [tenders, setTenders] = useState<AssignedTender[]>([]);
  const [stats, setStats] = useState({ pending: 0, completed: 0, total: 0 });

  // Bids modal / expansion
  const [expandedTender, setExpandedTender] = useState<number | null>(null);
  const [bids, setBids] = useState<Bid[]>([]);
  const [bidsLoading, setBidsLoading] = useState(false);

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      // Fetch tenders that are in evaluation-related statuses
      const tenderRes = await tenderApi.getAll({
        status: 'EVALUATION_IN_PROGRESS',
        page: 0,
        size: 50,
      });
      const tenderPage: Page<Tender> = tenderRes.data;

      // Fetch evaluations for each tender to compute stats
      const enriched: AssignedTender[] = [];
      let pendingCount = 0;
      let completedCount = 0;

      for (const t of tenderPage.content) {
        try {
          const evalRes = await evaluationApi.getByTender(t.id);
          const evals: Evaluation[] = evalRes.data ?? [];
          const completed = evals.filter((e) => e.status === 'COMPLETED').length;
          const pending = evals.filter((e) => e.status === 'PENDING' || e.status === 'IN_PROGRESS').length;
          pendingCount += pending;
          completedCount += completed;
          enriched.push({
            ...t,
            evaluationCount: evals.length,
            pendingBids: pending,
          });
        } catch {
          enriched.push({ ...t, evaluationCount: 0, pendingBids: 0 });
        }
      }

      setTenders(enriched);
      setStats({
        pending: pendingCount,
        completed: completedCount,
        total: pendingCount + completedCount,
      });
    } catch {
      message.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleExpandBids = async (tenderId: number) => {
    if (expandedTender === tenderId) {
      setExpandedTender(null);
      setBids([]);
      return;
    }
    setBidsLoading(true);
    setExpandedTender(tenderId);
    try {
      const res = await bidApi.getByTender(tenderId, { page: 0, size: 100 });
      const page: Page<Bid> = res.data;
      setBids(page.content);
    } catch {
      message.error('Failed to load bids');
      setBids([]);
    } finally {
      setBidsLoading(false);
    }
  };

  const tenderColumns = [
    {
      title: 'Tender ID',
      dataIndex: 'id',
      key: 'id',
      width: 90,
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
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
      render: (status: string) => {
        const color = status === 'EVALUATION_IN_PROGRESS' ? 'processing' : 'default';
        return <Tag color={color}>{status.replace(/_/g, ' ')}</Tag>;
      },
    },
    {
      title: 'Evaluations',
      dataIndex: 'evaluationCount',
      key: 'evaluationCount',
      width: 110,
      align: 'center' as const,
    },
    {
      title: 'Pending',
      dataIndex: 'pendingBids',
      key: 'pendingBids',
      width: 90,
      align: 'center' as const,
      render: (val: number) => (val > 0 ? <Tag color="orange">{val}</Tag> : <Tag color="green">0</Tag>),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 140,
      render: (_: unknown, record: AssignedTender) => (
        <Button
          type="primary"
          size="small"
          onClick={() => handleExpandBids(record.id)}
        >
          {expandedTender === record.id ? 'Hide Bids' : 'View Bids'}
        </Button>
      ),
    },
  ];

  const bidColumns = [
    {
      title: 'Bid ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: 'Bidder',
      key: 'bidder',
      render: (_: unknown, record: Bid) => record.tendererName ?? `Bidder #${record.tendererId}`,
    },
    {
      title: 'Total Price',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      width: 130,
      render: (val: number) => val?.toLocaleString(),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: string) => <Tag color="blue">{status}</Tag>,
    },
    {
      title: 'Submitted',
      dataIndex: 'submissionTime',
      key: 'submissionTime',
      width: 160,
      render: (val: string) => (val ? new Date(val).toLocaleDateString() : '-'),
    },
    {
      title: 'Action',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Bid) => (
        <Button
          type="link"
          onClick={() => navigate(`/evaluator/evaluate/${expandedTender}/${record.id}`)}
        >
          Evaluate
        </Button>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px 16px' }}>
      <Title level={3}>Evaluator Dashboard</Title>

      {/* Stat Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Pending Evaluations"
              value={stats.pending}
              prefix={<FileSearchOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Completed Evaluations"
              value={stats.completed}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Total Evaluations"
              value={stats.total}
              prefix={<UnorderedListOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Tenders Table */}
      <Card title="Tenders Assigned for Evaluation">
        <Table
          dataSource={tenders}
          columns={tenderColumns}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* Bids Sub-table */}
      {expandedTender && (
        <Card
          title={`Bids for Tender #${expandedTender}`}
          style={{ marginTop: 16 }}
        >
          <Table
            dataSource={bids}
            columns={bidColumns}
            rowKey="id"
            loading={bidsLoading}
            pagination={false}
          />
        </Card>
      )}
    </div>
  );
}
