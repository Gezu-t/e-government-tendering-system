import { useEffect, useState, useMemo } from 'react';
import {
  Card,
  Form,
  InputNumber,
  Input,
  Button,
  Upload,
  Typography,
  Descriptions,
  Table,
  Tag,
  Spin,
  Modal,
  message,
  Space,
  Divider,
  Alert,
} from 'antd';
import { UploadOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { tenderApi, bidApi } from '../../api/services';
import type { Tender, TenderCriteria } from '../../types';
import type { UploadFile } from 'antd/es/upload/interface';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

interface BidItemFormValue {
  criteriaId: number;
  value: number | null;
  description: string;
}

export default function SubmitBidPage() {
  const { tenderId } = useParams<{ tenderId: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const [tender, setTender] = useState<Tender | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [bidItems, setBidItems] = useState<BidItemFormValue[]>([]);

  useEffect(() => {
    const fetchTender = async () => {
      if (!tenderId) return;
      setLoading(true);
      try {
        const { data } = await tenderApi.getById(Number(tenderId));
        setTender(data);
        const items = (data.criteria || [])
          .filter((c) => c.active)
          .map((c) => ({
            criteriaId: c.id,
            value: null,
            description: '',
          }));
        setBidItems(items);
      } catch {
        message.error('Failed to load tender details.');
      }
      setLoading(false);
    };
    fetchTender();
  }, [tenderId]);

  const activeCriteria = useMemo(
    () => (tender?.criteria || []).filter((c) => c.active),
    [tender],
  );

  const totalPrice = useMemo(
    () => bidItems.reduce((sum, item) => sum + (item.value ?? 0), 0),
    [bidItems],
  );

  const handleItemChange = (
    index: number,
    field: 'value' | 'description',
    val: number | string | null,
  ) => {
    setBidItems((prev) => {
      const next = [...prev];
      if (field === 'value') {
        next[index] = { ...next[index], value: val as number | null };
      } else {
        next[index] = { ...next[index], description: val as string };
      }
      return next;
    });
  };

  const handleSubmit = async () => {
    const invalidItems = bidItems.filter((item) => item.value === null || item.value === undefined);
    if (invalidItems.length > 0) {
      message.warning('Please fill in values for all criteria.');
      return;
    }

    setSubmitting(true);
    try {
      const items = bidItems.map((item) => ({
        criteriaId: item.criteriaId,
        value: item.value!,
        description: item.description || undefined,
      }));

      // Create the bid
      const { data: createdBid } = await bidApi.create({
        tenderId: Number(tenderId),
        items,
      });

      // Upload documents if any
      for (const file of fileList) {
        if (file.originFileObj) {
          const formData = new FormData();
          formData.append('file', file.originFileObj);
          await bidApi.uploadDocument(createdBid.id, formData);
        }
      }

      // Submit (seal) the bid
      await bidApi.submit(createdBid.id);

      Modal.success({
        title: 'Bid Sealed and Submitted',
        icon: <CheckCircleOutlined />,
        content: (
          <div>
            <p>Your bid has been successfully sealed and submitted.</p>
            <p>
              <strong>Bid ID:</strong> #{createdBid.id}
            </p>
            <p>
              <strong>Total Price:</strong>{' '}
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(
                totalPrice,
              )}
            </p>
            <p>Your bid is now sealed and cannot be modified.</p>
          </div>
        ),
        onOk: () => navigate('/tenderer/dashboard'),
      });
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : 'Failed to submit bid. Please try again.';
      message.error(errorMsg);
    }
    setSubmitting(false);
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin />
      </div>
    );
  }

  if (!tender) {
    return (
      <Alert
        type="error"
        message="Tender Not Found"
        description="The requested tender could not be loaded."
        showIcon
        style={{ margin: 24 }}
      />
    );
  }

  const isDeadlinePassed = dayjs().isAfter(dayjs(tender.submissionDeadline));

  const criteriaColumns = [
    {
      title: 'Criteria',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: TenderCriteria) => (
        <div>
          <Text strong>{name}</Text>
          {record.description && (
            <div>
              <Text type="secondary">{record.description}</Text>
            </div>
          )}
        </div>
      ),
    },
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
      title: 'Value',
      key: 'value',
      render: (_: unknown, record: TenderCriteria) => {
        const idx = bidItems.findIndex((item) => item.criteriaId === record.id);
        if (idx === -1) return null;
        return (
          <InputNumber
            min={0}
            step={0.01}
            style={{ width: '100%' }}
            placeholder="Enter value"
            value={bidItems[idx].value}
            onChange={(val) => handleItemChange(idx, 'value', val)}
            disabled={isDeadlinePassed}
          />
        );
      },
    },
    {
      title: 'Description',
      key: 'description',
      render: (_: unknown, record: TenderCriteria) => {
        const idx = bidItems.findIndex((item) => item.criteriaId === record.id);
        if (idx === -1) return null;
        return (
          <Input
            placeholder="Optional description"
            value={bidItems[idx].description}
            onChange={(e) => handleItemChange(idx, 'description', e.target.value)}
            disabled={isDeadlinePassed}
          />
        );
      },
    },
  ];

  return (
    <div style={{ padding: 24, maxWidth: 1000, margin: '0 auto' }}>
      <Title level={3}>Submit Bid</Title>

      {isDeadlinePassed && (
        <Alert
          type="error"
          message="Submission Deadline Passed"
          description="The submission deadline for this tender has passed. You cannot submit a bid."
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Card title="Tender Information" style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
          <Descriptions.Item label="Title">{tender.title}</Descriptions.Item>
          <Descriptions.Item label="Type">
            <Tag>{tender.type}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color="green">{tender.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Deadline">
            {dayjs(tender.submissionDeadline).format('MMM D, YYYY HH:mm')}
          </Descriptions.Item>
          <Descriptions.Item label="Description" span={2}>
            {tender.description}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Bid Items" style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical">
          <Table
            columns={criteriaColumns}
            dataSource={activeCriteria}
            rowKey="id"
            pagination={false}
            size="small"
          />

          <Divider />

          <div style={{ textAlign: 'right', marginBottom: 16 }}>
            <Text strong style={{ fontSize: 16 }}>
              Total Price:{' '}
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(
                totalPrice,
              )}
            </Text>
          </div>
        </Form>
      </Card>

      <Card title="Supporting Documents" style={{ marginBottom: 16 }}>
        <Upload
          multiple
          fileList={fileList}
          onChange={({ fileList: newFileList }) => setFileList(newFileList)}
          beforeUpload={() => false}
        >
          <Button icon={<UploadOutlined />} disabled={isDeadlinePassed}>
            Select Files
          </Button>
        </Upload>
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          Upload any supporting documents such as business licenses, certifications, or technical
          proposals.
        </Text>
      </Card>

      <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button onClick={() => navigate(-1)}>Cancel</Button>
        <Button
          type="primary"
         
          loading={submitting}
          disabled={isDeadlinePassed || bidItems.length === 0}
          onClick={handleSubmit}
        >
          Submit Bid
        </Button>
      </Space>
    </div>
  );
}
