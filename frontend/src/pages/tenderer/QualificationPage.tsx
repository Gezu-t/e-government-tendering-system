import { useCallback, useEffect, useState } from 'react';
import {
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Button,
  Table,
  Tag,
  Typography,
  Spin,
  message,
  Row,
  Col,
  Alert,
  Space,
} from 'antd';
import { SafetyCertificateOutlined, PlusOutlined } from '@ant-design/icons';
import { userApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { VendorQualification, QualificationStatus, User } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { TextArea } = Input;

const qualificationStatusColors: Record<QualificationStatus, string> = {
  PENDING: 'blue',
  UNDER_REVIEW: 'orange',
  QUALIFIED: 'green',
  CONDITIONALLY_QUALIFIED: 'cyan',
  DISQUALIFIED: 'red',
  EXPIRED: 'default',
  SUSPENDED: 'volcano',
};

interface QualificationFormValues {
  category: string;
  businessLicenseNumber: string;
  taxRegistrationNumber: string;
  yearsOfExperience: number;
  annualRevenue: number;
  employeeCount: number;
  pastContractsCount: number;
  certificationDetails: string;
}

export default function QualificationPage() {
  const [form] = Form.useForm<QualificationFormValues>();
  const { userId } = useAuthStore();

  const [qualifications, setQualifications] = useState<VendorQualification[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [orgId, setOrgId] = useState<number | null>(null);
  const [qualified, setQualified] = useState<boolean | null>(null);
  const [showForm, setShowForm] = useState(false);

  const fetchData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const { data: user } = (await userApi.getById(userId)) as { data: User };
      const organizationId = user.organizations?.[0]?.organizationId;
      if (organizationId) {
        setOrgId(organizationId);
        const [qualsRes, qualifiedRes] = await Promise.all([
          userApi.getQualifications(organizationId),
          userApi.isQualified(organizationId),
        ]);
        setQualifications(qualsRes.data);
        setQualified(qualifiedRes.data);
      }
    } catch {
      message.error('Failed to load qualification data.');
    }
    setLoading(false);
  }, [userId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmit = async (values: QualificationFormValues) => {
    if (!orgId) {
      message.error('Organization not found. Please ensure your account is linked to an organization.');
      return;
    }
    setSubmitting(true);
    try {
      await userApi.submitQualification({
        organizationId: orgId,
        qualificationCategory: values.category,
        businessLicenseNumber: values.businessLicenseNumber,
        taxRegistrationNumber: values.taxRegistrationNumber,
        yearsOfExperience: values.yearsOfExperience,
        annualRevenue: values.annualRevenue,
        employeeCount: values.employeeCount,
        pastContractsCount: values.pastContractsCount,
        certificationDetails: values.certificationDetails,
      });
      message.success('Qualification submitted successfully.');
      form.resetFields();
      setShowForm(false);
      await fetchData();
    } catch {
      message.error('Failed to submit qualification.');
    }
    setSubmitting(false);
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'Category',
      dataIndex: 'qualificationCategory',
      key: 'qualificationCategory',
    },
    {
      title: 'Organization',
      dataIndex: 'organizationName',
      key: 'organizationName',
    },
    {
      title: 'Score',
      dataIndex: 'qualificationScore',
      key: 'qualificationScore',
      render: (score: number | undefined) => (score !== undefined && score !== null ? score : '-'),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: QualificationStatus) => (
        <Tag color={qualificationStatusColors[status]}>{status.replace(/_/g, ' ')}</Tag>
      ),
    },
    {
      title: 'Valid From',
      dataIndex: 'validFrom',
      key: 'validFrom',
      render: (d: string | undefined) => (d ? dayjs(d).format('MMM D, YYYY') : '-'),
    },
    {
      title: 'Valid Until',
      dataIndex: 'validUntil',
      key: 'validUntil',
      render: (d: string | undefined) => (d ? dayjs(d).format('MMM D, YYYY') : '-'),
    },
    {
      title: 'Submitted',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (d: string) => dayjs(d).format('MMM D, YYYY'),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>
        <SafetyCertificateOutlined style={{ marginRight: 8 }} />
        Vendor Pre-Qualification
      </Title>

      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title="Current Qualification Status">
            {qualified === true ? (
              <Alert
                message="Qualified"
                description="Your organization is currently pre-qualified to participate in tenders."
                type="success"
                showIcon
              />
            ) : qualified === false ? (
              <Alert
                message="Not Qualified"
                description="Your organization is not currently pre-qualified. Submit a qualification application below."
                type="warning"
                showIcon
              />
            ) : (
              <Alert
                message="No Organization"
                description="Your account is not linked to an organization. Please contact an administrator."
                type="info"
                showIcon
              />
            )}
          </Card>
        </Col>

        <Col span={24}>
          <Card
            title="Qualifications"
            extra={
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setShowForm(!showForm)}
              >
                {showForm ? 'Hide Form' : 'New Qualification'}
              </Button>
            }
          >
            {showForm && (
              <Card
                type="inner"
                title="Submit New Qualification"
                style={{ marginBottom: 16 }}
              >
                <Form
                  form={form}
                  layout="vertical"
                  onFinish={handleSubmit}
                  style={{ maxWidth: 800 }}
                >
                  <Row gutter={16}>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="category"
                        label="Qualification Category"
                        rules={[{ required: true, message: 'Please select a category' }]}
                      >
                        <Select
                          placeholder="Select category"
                          options={[
                            { value: 'CONSTRUCTION', label: 'Construction' },
                            { value: 'IT_SERVICES', label: 'IT Services' },
                            { value: 'CONSULTING', label: 'Consulting' },
                            { value: 'SUPPLY', label: 'Supply' },
                            { value: 'MAINTENANCE', label: 'Maintenance' },
                            { value: 'PROFESSIONAL_SERVICES', label: 'Professional Services' },
                            { value: 'OTHER', label: 'Other' },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="businessLicenseNumber"
                        label="Business License Number"
                        rules={[{ required: true, message: 'Please enter business license number' }]}
                      >
                        <Input placeholder="e.g. BL-2024-001234" />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={16}>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="taxRegistrationNumber"
                        label="Tax Registration Number"
                        rules={[{ required: true, message: 'Please enter tax registration number' }]}
                      >
                        <Input placeholder="e.g. TIN-123456789" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="yearsOfExperience"
                        label="Years of Experience"
                        rules={[{ required: true, message: 'Please enter years of experience' }]}
                      >
                        <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="e.g. 10" />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={16}>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="annualRevenue"
                        label="Annual Revenue (USD)"
                        rules={[{ required: true, message: 'Please enter annual revenue' }]}
                      >
                        <InputNumber
                          min={0}
                          step={1000}
                          style={{ width: '100%' }}
                          formatter={(value) =>
                            `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                          }
                          parser={(value) => Number(value?.replace(/,/g, '') ?? 0) as unknown as 0}
                          placeholder="e.g. 1,000,000"
                        />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="employeeCount"
                        label="Number of Employees"
                        rules={[{ required: true, message: 'Please enter employee count' }]}
                      >
                        <InputNumber min={1} style={{ width: '100%' }} placeholder="e.g. 50" />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={16}>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="pastContractsCount"
                        label="Past Contracts Count"
                        rules={[{ required: true, message: 'Please enter past contracts count' }]}
                      >
                        <InputNumber min={0} style={{ width: '100%' }} placeholder="e.g. 15" />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Form.Item
                    name="certificationDetails"
                    label="Certification Details"
                    rules={[{ required: true, message: 'Please enter certification details' }]}
                  >
                    <TextArea
                      rows={4}
                      placeholder="List relevant certifications, e.g. ISO 9001, ISO 27001, industry-specific certifications..."
                    />
                  </Form.Item>

                  <Space>
                    <Button type="primary" htmlType="submit" loading={submitting}>
                      Submit Qualification
                    </Button>
                    <Button onClick={() => { setShowForm(false); form.resetFields(); }}>
                      Cancel
                    </Button>
                  </Space>
                </Form>
              </Card>
            )}

            <Table
              columns={columns}
              dataSource={qualifications}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              locale={{ emptyText: 'No qualifications submitted yet.' }}
            />

            {qualifications.length > 0 && (
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  Showing {qualifications.length} qualification(s) for your organization.
                </Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
