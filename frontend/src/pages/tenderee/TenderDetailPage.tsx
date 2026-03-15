import { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Descriptions,
  Tag,
  Button,
  Tabs,
  Table,
  Space,
  Typography,
  Modal,
  Form,
  Input,
  DatePicker,
  message,
  Spin,
  List,
  Empty,
  Popconfirm,
} from 'antd';
import {
  SendOutlined,
  StopOutlined,
  EditOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { tenderApi, bidApi } from '../../api/services';
import type {
  Tender,
  TenderStatus,
  TenderAmendment,
  TenderCriteria,
  Bid,
  BidStatus,
} from '../../types';
import dayjs from 'dayjs';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

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

const bidStatusColors: Record<BidStatus, string> = {
  DRAFT: 'default',
  SUBMITTED: 'processing',
  UNDER_EVALUATION: 'blue',
  ACCEPTED: 'green',
  REJECTED: 'red',
  EVALUATED: 'cyan',
  AWARDED: 'gold',
  CONTRACTED: 'purple',
};

interface Clarification {
  id: number;
  question: string;
  answer?: string;
  category?: string;
  organizationName?: string;
  status: string;
  createdAt: string;
  answeredAt?: string;
}

export default function TenderDetailPage() {
  const { tenderId } = useParams<{ tenderId: string }>();
  const navigate = useNavigate();
  const id = Number(tenderId);

  const [tender, setTender] = useState<Tender | null>(null);
  const [amendments, setAmendments] = useState<TenderAmendment[]>([]);
  const [clarifications, setClarifications] = useState<Clarification[]>([]);
  const [bids, setBids] = useState<Bid[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [amendModalOpen, setAmendModalOpen] = useState(false);
  const [answerModalOpen, setAnswerModalOpen] = useState(false);
  const [selectedClarification, setSelectedClarification] = useState<Clarification | null>(null);

  const [amendForm] = Form.useForm();
  const [answerForm] = Form.useForm();

  const fetchTender = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await tenderApi.getById(id);
      setTender(data);
    } catch {
      message.error('Failed to load tender');
    }
    setLoading(false);
  }, [id]);

  const fetchAmendments = useCallback(async () => {
    try {
      const { data } = await tenderApi.getAmendments(id);
      setAmendments(data);
    } catch {
      /* silent */
    }
  }, [id]);

  const fetchClarifications = useCallback(async () => {
    try {
      const { data } = await tenderApi.getClarifications(id);
      setClarifications(data as Clarification[]);
    } catch {
      /* silent */
    }
  }, [id]);

  const fetchBids = useCallback(async () => {
    try {
      const { data } = await bidApi.getByTender(id);
      setBids(data.content);
    } catch {
      /* silent */
    }
  }, [id]);

  useEffect(() => {
    if (!id) return;
    fetchTender();
    fetchAmendments();
    fetchClarifications();
  }, [id, fetchTender, fetchAmendments, fetchClarifications]);

  useEffect(() => {
    if (
      tender &&
      tender.status !== 'DRAFT' &&
      tender.status !== 'PUBLISHED' &&
      tender.status !== 'AMENDED'
    ) {
      fetchBids();
    }
  }, [tender, fetchBids]);

  const handlePublish = async () => {
    setActionLoading(true);
    try {
      await tenderApi.publish(id);
      message.success('Tender published successfully');
      fetchTender();
    } catch {
      message.error('Failed to publish tender');
    }
    setActionLoading(false);
  };

  const handleClose = async () => {
    setActionLoading(true);
    try {
      await tenderApi.close(id);
      message.success('Tender closed successfully');
      fetchTender();
    } catch {
      message.error('Failed to close tender');
    }
    setActionLoading(false);
  };

  const handleAmend = async () => {
    try {
      const values = await amendForm.validateFields();
      setActionLoading(true);
      await tenderApi.amend(id, {
        reason: values.reason,
        description: values.description,
        newSubmissionDeadline: values.newDeadline?.toISOString(),
      });
      message.success('Amendment submitted successfully');
      setAmendModalOpen(false);
      amendForm.resetFields();
      fetchTender();
      fetchAmendments();
    } catch {
      /* validation or api error */
    }
    setActionLoading(false);
  };

  const handleAnswer = async () => {
    if (!selectedClarification) return;
    try {
      const values = await answerForm.validateFields();
      setActionLoading(true);
      await tenderApi.answerQuestion(id, selectedClarification.id, {
        answer: values.answer,
        makePublic: true,
      });
      message.success('Answer submitted');
      setAnswerModalOpen(false);
      answerForm.resetFields();
      setSelectedClarification(null);
      fetchClarifications();
    } catch {
      /* validation or api error */
    }
    setActionLoading(false);
  };

  const renderActionButtons = () => {
    if (!tender) return null;
    const buttons: React.ReactNode[] = [];

    if (tender.status === 'DRAFT') {
      buttons.push(
        <Popconfirm
          key="publish"
          title="Publish this tender?"
          description="Once published, bidders will be able to view and submit bids."
          onConfirm={handlePublish}
        >
          <Button type="primary" icon={<SendOutlined />} loading={actionLoading}>
            Publish
          </Button>
        </Popconfirm>
      );
    }

    if (tender.status === 'PUBLISHED' || tender.status === 'AMENDED') {
      buttons.push(
        <Popconfirm
          key="close"
          title="Close this tender?"
          description="No more bids will be accepted after closing."
          onConfirm={handleClose}
        >
          <Button danger icon={<StopOutlined />} loading={actionLoading}>
            Close Tender
          </Button>
        </Popconfirm>
      );
      buttons.push(
        <Button
          key="amend"
          icon={<EditOutlined />}
          onClick={() => setAmendModalOpen(true)}
        >
          Amend
        </Button>
      );
    }

    return buttons.length > 0 ? <Space>{buttons}</Space> : null;
  };

  const renderDetailsTab = () => (
    <Descriptions bordered column={2}>
      <Descriptions.Item label="Title">{tender?.title}</Descriptions.Item>
      <Descriptions.Item label="Status">
        <Tag color={statusColors[tender?.status ?? 'DRAFT']}>
          {tender?.status?.replace(/_/g, ' ')}
        </Tag>
      </Descriptions.Item>
      <Descriptions.Item label="Description" span={2}>
        <Paragraph>{tender?.description}</Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="Type">
        <Tag>{tender?.type}</Tag>
      </Descriptions.Item>
      <Descriptions.Item label="Allocation Strategy">
        <Tag color="blue">{tender?.allocationStrategy}</Tag>
      </Descriptions.Item>
      <Descriptions.Item label="Submission Deadline">
        {tender?.submissionDeadline
          ? dayjs(tender.submissionDeadline).format('MMM D, YYYY HH:mm')
          : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="Created">
        {tender?.createdAt ? dayjs(tender.createdAt).format('MMM D, YYYY HH:mm') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="Last Updated">
        {tender?.updatedAt ? dayjs(tender.updatedAt).format('MMM D, YYYY HH:mm') : '-'}
      </Descriptions.Item>
    </Descriptions>
  );

  const criteriaColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: 'Weight',
      dataIndex: 'weight',
      key: 'weight',
      render: (w: number) => `${w}%`,
    },
    {
      title: 'Prefer Higher',
      dataIndex: 'preferHigher',
      key: 'preferHigher',
      render: (v: boolean) => (v ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>),
    },
    {
      title: 'Active',
      dataIndex: 'active',
      key: 'active',
      render: (v: boolean) =>
        v ? <Tag color="green">Active</Tag> : <Tag color="red">Inactive</Tag>,
    },
  ];

  const itemColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Quantity', dataIndex: 'quantity', key: 'quantity' },
    { title: 'Unit', dataIndex: 'unit', key: 'unit' },
    {
      title: 'Estimated Price',
      dataIndex: 'estimatedPrice',
      key: 'estimatedPrice',
      render: (v: number | undefined) => (v != null ? `$${v.toLocaleString()}` : '-'),
    },
    {
      title: 'Criterion',
      dataIndex: 'criteriaId',
      key: 'criteriaId',
      render: (cid: number) => {
        const criterion = tender?.criteria?.find((c: TenderCriteria) => c.id === cid);
        return criterion ? criterion.name : cid;
      },
    },
  ];

  const renderCriteriaItemsTab = () => (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Title level={5}>Evaluation Criteria</Title>
        <Table
          columns={criteriaColumns}
          dataSource={tender?.criteria ?? []}
          rowKey="id"
          pagination={false}
          size="small"
        />
      </div>
      <div>
        <Title level={5}>Tender Items</Title>
        <Table
          columns={itemColumns}
          dataSource={tender?.items ?? []}
          rowKey="id"
          pagination={false}
          size="small"
        />
      </div>
    </Space>
  );

  const renderAmendmentsTab = () => (
    <div>
      {amendments.length === 0 ? (
        <Empty description="No amendments" />
      ) : (
        <List
          itemLayout="vertical"
          dataSource={amendments}
          renderItem={(amendment) => (
            <List.Item key={amendment.id}>
              <List.Item.Meta
                title={
                  <Space>
                    <Tag color="purple">Amendment #{amendment.amendmentNumber}</Tag>
                    <Text type="secondary">
                      {dayjs(amendment.createdAt).format('MMM D, YYYY HH:mm')}
                    </Text>
                  </Space>
                }
                description={amendment.reason}
              />
              {amendment.description && <Paragraph>{amendment.description}</Paragraph>}
              {amendment.newDeadline && (
                <Text>
                  New deadline:{' '}
                  <Text strong>{dayjs(amendment.newDeadline).format('MMM D, YYYY HH:mm')}</Text>
                  {amendment.previousDeadline && (
                    <Text type="secondary" delete style={{ marginLeft: 8 }}>
                      {dayjs(amendment.previousDeadline).format('MMM D, YYYY HH:mm')}
                    </Text>
                  )}
                </Text>
              )}
            </List.Item>
          )}
        />
      )}
    </div>
  );

  const renderClarificationsTab = () => (
    <div>
      {clarifications.length === 0 ? (
        <Empty description="No clarifications yet" />
      ) : (
        <List
          itemLayout="vertical"
          dataSource={clarifications}
          renderItem={(item) => (
            <List.Item
              key={item.id}
              actions={
                !item.answer
                  ? [
                      <Button
                        key="answer"
                        type="primary"
                        size="small"
                        onClick={() => {
                          setSelectedClarification(item);
                          setAnswerModalOpen(true);
                        }}
                      >
                        Answer
                      </Button>,
                    ]
                  : undefined
              }
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Text strong>Q:</Text>
                    <Text>{item.question}</Text>
                    {!item.answer && <Tag color="orange">Pending</Tag>}
                    {item.answer && <Tag color="green">Answered</Tag>}
                  </Space>
                }
                description={
                  <Space>
                    {item.organizationName && <Tag>{item.organizationName}</Tag>}
                    {item.category && <Tag color="blue">{item.category}</Tag>}
                    <Text type="secondary">
                      {dayjs(item.createdAt).format('MMM D, YYYY HH:mm')}
                    </Text>
                  </Space>
                }
              />
              {item.answer && (
                <div style={{ marginLeft: 24 }}>
                  <Text strong>A: </Text>
                  <Text>{item.answer}</Text>
                  {item.answeredAt && (
                    <Text type="secondary" style={{ marginLeft: 8 }}>
                      ({dayjs(item.answeredAt).format('MMM D, YYYY HH:mm')})
                    </Text>
                  )}
                </div>
              )}
            </List.Item>
          )}
        />
      )}
    </div>
  );

  const bidColumns = [
    {
      title: 'Bidder',
      dataIndex: 'tendererName',
      key: 'tendererName',
      render: (name: string | undefined, record: Bid) => name || `Bidder #${record.tendererId}`,
    },
    {
      title: 'Total Price',
      dataIndex: 'totalPrice',
      key: 'totalPrice',
      render: (v: number) => `$${v.toLocaleString()}`,
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
      title: 'Items',
      dataIndex: 'items',
      key: 'items',
      render: (items: unknown[]) => items?.length ?? 0,
    },
  ];

  const renderBidsTab = () => {
    const canViewBids =
      tender &&
      tender.status !== 'DRAFT' &&
      tender.status !== 'PUBLISHED' &&
      tender.status !== 'AMENDED';

    if (!canViewBids) {
      return (
        <Empty description="Bids will be visible after the tender is closed for submissions." />
      );
    }

    return (
      <Table
        columns={bidColumns}
        dataSource={bids}
        rowKey="id"
        size="middle"
        pagination={{ pageSize: 10 }}
      />
    );
  };

  const tabItems = [
    { key: 'details', label: 'Details', children: renderDetailsTab() },
    { key: 'criteria', label: 'Criteria & Items', children: renderCriteriaItemsTab() },
    { key: 'amendments', label: `Amendments (${amendments.length})`, children: renderAmendmentsTab() },
    { key: 'clarifications', label: `Clarifications (${clarifications.length})`, children: renderClarificationsTab() },
    { key: 'bids', label: `Bids (${bids.length})`, children: renderBidsTab() },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!tender) {
    return (
      <Card>
        <Empty description="Tender not found">
          <Button type="primary" onClick={() => navigate('/tenders')}>
            Back to Tenders
          </Button>
        </Empty>
      </Card>
    );
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between', flexWrap: 'wrap' }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
            <Title level={3} style={{ margin: 0 }}>
              {tender.title}
            </Title>
            <Tag color={statusColors[tender.status]} style={{ fontSize: 14, padding: '2px 12px' }}>
              {tender.status.replace(/_/g, ' ')}
            </Tag>
          </Space>
          {renderActionButtons()}
        </Space>
      </Card>

      <Card>
        <Tabs items={tabItems} defaultActiveKey="details" />
      </Card>

      {/* Amend Modal */}
      <Modal
        title="Amend Tender"
        open={amendModalOpen}
        onOk={handleAmend}
        onCancel={() => {
          setAmendModalOpen(false);
          amendForm.resetFields();
        }}
        confirmLoading={actionLoading}
        okText="Submit Amendment"
      >
        <Form form={amendForm} layout="vertical">
          <Form.Item
            name="reason"
            label="Reason for Amendment"
            rules={[{ required: true, message: 'Please provide a reason' }]}
          >
            <TextArea rows={3} placeholder="Describe why this tender needs to be amended" />
          </Form.Item>
          <Form.Item name="description" label="Additional Description">
            <TextArea rows={2} placeholder="Optional details about the amendment" />
          </Form.Item>
          <Form.Item name="newDeadline" label="New Submission Deadline (optional)">
            <DatePicker
              showTime
              style={{ width: '100%' }}
              disabledDate={(d) => d.isBefore(dayjs(), 'day')}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Answer Clarification Modal */}
      <Modal
        title="Answer Clarification"
        open={answerModalOpen}
        onOk={handleAnswer}
        onCancel={() => {
          setAnswerModalOpen(false);
          answerForm.resetFields();
          setSelectedClarification(null);
        }}
        confirmLoading={actionLoading}
        okText="Submit Answer"
      >
        {selectedClarification && (
          <div style={{ marginBottom: 16 }}>
            <Text strong>Question:</Text>
            <Paragraph style={{ marginTop: 4, background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
              {selectedClarification.question}
            </Paragraph>
          </div>
        )}
        <Form form={answerForm} layout="vertical">
          <Form.Item
            name="answer"
            label="Your Answer"
            rules={[{ required: true, message: 'Please provide an answer' }]}
          >
            <TextArea rows={4} placeholder="Type your answer here" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
