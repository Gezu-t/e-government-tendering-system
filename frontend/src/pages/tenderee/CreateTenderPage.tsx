import { useState } from 'react';
import {
  Steps,
  Form,
  Input,
  Select,
  DatePicker,
  InputNumber,
  Button,
  Card,
  Space,
  Checkbox,
  Descriptions,
  Tag,
  Typography,
  Divider,
Flex, } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { tenderApi } from '../../api/services';
import { useMessage } from '../../hooks/useMessage';
import type { TenderType, AllocationStrategy, CriteriaType } from '../../types';
import dayjs from 'dayjs';

const { Title } = Typography;
const { TextArea } = Input;

interface CriteriaFormValue {
  name: string;
  type: CriteriaType;
  weight: number;
  preferHigher?: boolean;
}

interface ItemFormValue {
  name: string;
  quantity: number;
  unit: string;
  estimatedPrice: number;
  criteriaId: number;
}

interface BasicFormValue {
  title: string;
  description: string;
  type: TenderType;
  submissionDeadline: dayjs.Dayjs;
  allocationStrategy: AllocationStrategy;
}

const tenderTypeOptions: { value: TenderType; label: string }[] = [
  { value: 'OPEN', label: 'Open' },
  { value: 'SELECTIVE', label: 'Selective' },
  { value: 'LIMITED', label: 'Limited' },
  { value: 'SINGLE', label: 'Single Source' },
];

const allocationOptions: { value: AllocationStrategy; label: string }[] = [
  { value: 'SINGLE', label: 'Single Winner' },
  { value: 'COOPERATIVE', label: 'Cooperative' },
  { value: 'COMPETITIVE', label: 'Competitive' },
];

const criteriaTypeOptions: { value: CriteriaType; label: string }[] = [
  { value: 'PRICE', label: 'Price' },
  { value: 'QUANTITY', label: 'Quantity' },
  { value: 'TIME', label: 'Time' },
  { value: 'QUALITY', label: 'Quality' },
  { value: 'ENUMERATION', label: 'Enumeration' },
  { value: 'EXPERIENCE', label: 'Experience' },
];

export default function CreateTenderPage() {
  const navigate = useNavigate();
  const message = useMessage();
  const [current, setCurrent] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const [basicForm] = Form.useForm<BasicFormValue>();
  const [criteriaForm] = Form.useForm<{ criteria: CriteriaFormValue[] }>();
  const [itemsForm] = Form.useForm<{ items: ItemFormValue[] }>();

  const [basicValues, setBasicValues] = useState<BasicFormValue | null>(null);
  const [criteriaValues, setCriteriaValues] = useState<CriteriaFormValue[]>([]);
  const [itemValues, setItemValues] = useState<ItemFormValue[]>([]);

  const steps = [
    { title: 'Basic Info' },
    { title: 'Criteria' },
    { title: 'Items' },
    { title: 'Review & Submit' },
  ];

  const nextStep = async () => {
    try {
      if (current === 0) {
        const values = await basicForm.validateFields();
        setBasicValues(values);
      } else if (current === 1) {
        const values = await criteriaForm.validateFields();
        setCriteriaValues(values.criteria || []);
      } else if (current === 2) {
        const values = await itemsForm.validateFields();
        setItemValues(values.items || []);
      }
      setCurrent(current + 1);
    } catch {
      /* validation errors displayed by form */
    }
  };

  const prevStep = () => {
    setCurrent(current - 1);
  };

  const handleSubmit = async () => {
    if (!basicValues) return;
    setSubmitting(true);
    try {
      const payload = {
        title: basicValues.title,
        description: basicValues.description,
        type: basicValues.type,
        submissionDeadline: basicValues.submissionDeadline.toISOString(),
        allocationStrategy: basicValues.allocationStrategy,
        criteria: criteriaValues.map((c, idx) => ({
          name: c.name,
          type: c.type,
          weight: c.weight,
          preferHigher: c.preferHigher ?? false,
          active: true,
          id: idx,
        })),
        items: itemValues.map((item) => ({
          name: item.name,
          quantity: item.quantity,
          unit: item.unit,
          estimatedPrice: item.estimatedPrice,
          criteriaId: item.criteriaId,
        })),
      };
      await tenderApi.create(payload);
      message.success('Tender created successfully');
      navigate('/tenders');
    } catch {
      message.error('Failed to create tender');
    }
    setSubmitting(false);
  };

  const renderBasicInfoStep = () => (
    <Form form={basicForm} layout="vertical" initialValues={basicValues || undefined}>
      <Form.Item
        name="title"
        label="Tender Title"
        rules={[{ required: true, message: 'Please enter a title' }]}
      >
        <Input placeholder="Enter tender title" maxLength={200} />
      </Form.Item>
      <Form.Item
        name="description"
        label="Description"
        rules={[{ required: true, message: 'Please enter a description' }]}
      >
        <TextArea rows={4} placeholder="Describe the tender requirements" />
      </Form.Item>
      <Form.Item
        name="type"
        label="Tender Type"
        rules={[{ required: true, message: 'Please select a type' }]}
      >
        <Select options={tenderTypeOptions} placeholder="Select tender type" />
      </Form.Item>
      <Form.Item
        name="submissionDeadline"
        label="Submission Deadline"
        rules={[{ required: true, message: 'Please select a deadline' }]}
      >
        <DatePicker
          showTime
          style={{ width: '100%' }}
          disabledDate={(d) => d.isBefore(dayjs(), 'day')}
        />
      </Form.Item>
      <Form.Item
        name="allocationStrategy"
        label="Allocation Strategy"
        rules={[{ required: true, message: 'Please select an allocation strategy' }]}
      >
        <Select options={allocationOptions} placeholder="Select allocation strategy" />
      </Form.Item>
    </Form>
  );

  const renderCriteriaStep = () => (
    <Form
      form={criteriaForm}
      layout="vertical"
      initialValues={{ criteria: criteriaValues.length > 0 ? criteriaValues : [{}] }}
    >
      <Form.List name="criteria">
        {(fields, { add, remove }) => (
          <>
            {fields.map(({ key, name, ...restField }) => (
              <Card
                key={key}
                size="small"
                style={{ marginBottom: 16 }}
                extra={
                  fields.length > 1 ? (
                    <MinusCircleOutlined
                      style={{ color: '#ff4d4f' }}
                      onClick={() => remove(name)}
                    />
                  ) : null
                }
                title={`Criterion ${name + 1}`}
              >
                <Space style={{ display: 'flex', flexWrap: 'wrap' }} align="start">
                  <Form.Item
                    {...restField}
                    name={[name, 'name']}
                    label="Name"
                    rules={[{ required: true, message: 'Required' }]}
                  >
                    <Input placeholder="e.g. Unit Price" style={{ width: 200 }} />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'type']}
                    label="Type"
                    rules={[{ required: true, message: 'Required' }]}
                  >
                    <Select
                      options={criteriaTypeOptions}
                      placeholder="Select"
                      style={{ width: 150 }}
                    />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'weight']}
                    label="Weight (%)"
                    rules={[
                      { required: true, message: 'Required' },
                      { type: 'number', min: 1, max: 100, message: '1-100' },
                    ]}
                  >
                    <InputNumber min={1} max={100} style={{ width: 100 }} />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'preferHigher']}
                    label="Prefer Higher"
                    valuePropName="checked"
                  >
                    <Checkbox />
                  </Form.Item>
                </Space>
              </Card>
            ))}
            <Form.Item>
              <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                Add Criterion
              </Button>
            </Form.Item>
          </>
        )}
      </Form.List>
    </Form>
  );

  const renderItemsStep = () => {
    const criteriaOptions = criteriaValues.map((c, idx) => ({
      value: idx,
      label: c.name || `Criterion ${idx + 1}`,
    }));

    return (
      <Form
        form={itemsForm}
        layout="vertical"
        initialValues={{ items: itemValues.length > 0 ? itemValues : [{}] }}
      >
        <Form.List name="items">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...restField }) => (
                <Card
                  key={key}
                  size="small"
                  style={{ marginBottom: 16 }}
                  extra={
                    fields.length > 1 ? (
                      <MinusCircleOutlined
                        style={{ color: '#ff4d4f' }}
                        onClick={() => remove(name)}
                      />
                    ) : null
                  }
                  title={`Item ${name + 1}`}
                >
                  <Space style={{ display: 'flex', flexWrap: 'wrap' }} align="start">
                    <Form.Item
                      {...restField}
                      name={[name, 'name']}
                      label="Item Name"
                      rules={[{ required: true, message: 'Required' }]}
                    >
                      <Input placeholder="e.g. Laptop" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'quantity']}
                      label="Quantity"
                      rules={[{ required: true, message: 'Required' }]}
                    >
                      <InputNumber min={1} style={{ width: 100 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'unit']}
                      label="Unit"
                      rules={[{ required: true, message: 'Required' }]}
                    >
                      <Input placeholder="e.g. pcs" style={{ width: 100 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'estimatedPrice']}
                      label="Est. Price"
                      rules={[{ required: true, message: 'Required' }]}
                    >
                      <InputNumber
                        min={0}
                        style={{ width: 140 }}
                        formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                        parser={(v) => Number(v?.replace(/,/g, '') ?? 0) as unknown as 0}
                        prefix="$"
                      />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'criteriaId']}
                      label="Criterion"
                      rules={[{ required: true, message: 'Required' }]}
                    >
                      <Select
                        options={criteriaOptions}
                        placeholder="Select criterion"
                        style={{ width: 180 }}
                      />
                    </Form.Item>
                  </Space>
                </Card>
              ))}
              <Form.Item>
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  Add Item
                </Button>
              </Form.Item>
            </>
          )}
        </Form.List>
      </Form>
    );
  };

  const renderReviewStep = () => (
    <Flex vertical gap={24} style={{ width: '100%' }}>
      <Descriptions title="Basic Information" bordered column={2}>
        <Descriptions.Item label="Title">{basicValues?.title}</Descriptions.Item>
        <Descriptions.Item label="Type">
          <Tag>{basicValues?.type}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Description" span={2}>
          {basicValues?.description}
        </Descriptions.Item>
        <Descriptions.Item label="Deadline">
          {basicValues?.submissionDeadline
            ? dayjs(basicValues.submissionDeadline).format('MMM D, YYYY HH:mm')
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="Allocation Strategy">
          <Tag color="blue">{basicValues?.allocationStrategy}</Tag>
        </Descriptions.Item>
      </Descriptions>

      <Divider />

      <Title level={5}>Criteria ({criteriaValues.length})</Title>
      {criteriaValues.map((c, idx) => (
        <Descriptions key={idx} size="small" bordered column={4} style={{ marginBottom: 8 }}>
          <Descriptions.Item label="Name">{c.name}</Descriptions.Item>
          <Descriptions.Item label="Type">
            <Tag>{c.type}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Weight">{c.weight}%</Descriptions.Item>
          <Descriptions.Item label="Prefer Higher">
            {c.preferHigher ? 'Yes' : 'No'}
          </Descriptions.Item>
        </Descriptions>
      ))}

      <Divider />

      <Title level={5}>Items ({itemValues.length})</Title>
      {itemValues.map((item, idx) => (
        <Descriptions key={idx} size="small" bordered column={4} style={{ marginBottom: 8 }}>
          <Descriptions.Item label="Name">{item.name}</Descriptions.Item>
          <Descriptions.Item label="Qty">
            {item.quantity} {item.unit}
          </Descriptions.Item>
          <Descriptions.Item label="Est. Price">
            ${item.estimatedPrice?.toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="Criterion">
            {criteriaValues[item.criteriaId]?.name ?? '-'}
          </Descriptions.Item>
        </Descriptions>
      ))}
    </Flex>
  );

  const stepContent = [renderBasicInfoStep, renderCriteriaStep, renderItemsStep, renderReviewStep];

  return (
    <Card>
      <Title level={3}>Create New Tender</Title>
      <Steps current={current} items={steps} style={{ marginBottom: 32 }} />

      <div style={{ minHeight: 300, marginBottom: 24 }}>{stepContent[current]()}</div>

      <div style={{ textAlign: 'right' }}>
        <Space>
          {current > 0 && <Button onClick={prevStep}>Previous</Button>}
          {current < steps.length - 1 && (
            <Button type="primary" onClick={nextStep}>
              Next
            </Button>
          )}
          {current === steps.length - 1 && (
            <Button type="primary" loading={submitting} onClick={handleSubmit}>
              Submit Tender
            </Button>
          )}
        </Space>
      </div>
    </Card>
  );
}
