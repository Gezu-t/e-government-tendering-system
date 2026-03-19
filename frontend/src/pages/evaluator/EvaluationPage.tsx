import { useState, useEffect, useMemo } from 'react';
import {
  Card, Form, Slider, InputNumber, Input, Button, Typography, Space,
  Checkbox, Alert, Spin, Divider, Tag, Descriptions,
Flex, } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import { evaluationApi, tenderApi, bidApi } from '../../api/services';
import { useMessage } from '../../hooks/useMessage';
import type { Tender, Bid, TenderCriteria } from '../../types';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface CriteriaFormValue {
  score: number;
  justification: string;
}

export default function EvaluationPage() {
  const { tenderId: tenderIdParam, bidId: bidIdParam } = useParams<{ tenderId: string; bidId: string }>();
  const navigate = useNavigate();
  const message = useMessage();
  const [form] = Form.useForm();

  const [tender, setTender] = useState<Tender | null>(null);
  const [bid, setBid] = useState<Bid | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Conflict of interest
  const [conflictDeclared, setConflictDeclared] = useState(false);
  const [hasConflict, setHasConflict] = useState(false);
  const [conflictDescription, setConflictDescription] = useState('');
  const [declaringConflict, setDeclaringConflict] = useState(false);

  // Per-criteria scores for live calculation
  const [scores, setScores] = useState<Record<number, number>>({});

  const tenderId = tenderIdParam ? Number(tenderIdParam) : null;
  const bidId = bidIdParam ? Number(bidIdParam) : null;

  useEffect(() => {
    if (!tenderId || !bidId) return;
    const fetchData = async () => {
      setLoading(true);
      try {
        const [tenderRes, bidRes] = await Promise.all([
          tenderApi.getById(tenderId),
          bidApi.getById(bidId),
        ]);
        setTender(tenderRes.data);
        setBid(bidRes.data);
      } catch {
        message.error('Failed to load tender or bid data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [tenderId, bidId]);

  const activeCriteria = useMemo(
    () => (tender?.criteria ?? []).filter((c) => c.active),
    [tender],
  );

  const totalWeight = useMemo(
    () => activeCriteria.reduce((sum, c) => sum + c.weight, 0),
    [activeCriteria],
  );

  const weightedScore = useMemo(() => {
    if (activeCriteria.length === 0 || totalWeight === 0) return 0;
    let sum = 0;
    for (const c of activeCriteria) {
      const s = scores[c.id] ?? 0;
      sum += (s / 10) * c.weight;
    }
    return Number(((sum / totalWeight) * 100).toFixed(2));
  }, [scores, activeCriteria, totalWeight]);

  const handleDeclareConflict = async () => {
    if (!tenderId) return;
    setDeclaringConflict(true);
    try {
      await evaluationApi.declareConflict(tenderId, {
        hasConflict,
        conflictDescription: hasConflict ? conflictDescription : undefined,
      });
      if (hasConflict) {
        message.warning('You declared a conflict of interest. You cannot evaluate this tender.');
      } else {
        message.success('No conflict of interest declared. You may proceed.');
      }
      setConflictDeclared(true);
    } catch {
      message.error('Failed to submit conflict of interest declaration');
    } finally {
      setDeclaringConflict(false);
    }
  };

  const handleScoreChange = (criteriaId: number, value: number) => {
    setScores((prev) => ({ ...prev, [criteriaId]: value }));
    form.setFieldValue(['criteria', criteriaId, 'score'], value);
  };

  const handleSubmit = async (values: {
    comments?: string;
    criteria: Record<number, CriteriaFormValue>;
  }) => {
    if (!tenderId || !bidId) return;
    setSubmitting(true);
    try {
      const criteriaScores = activeCriteria.map((c) => ({
        criteriaId: c.id,
        score: values.criteria[c.id]?.score ?? 0,
        justification: values.criteria[c.id]?.justification,
      }));

      await evaluationApi.create(tenderId, {
        bidId,
        criteriaScores,
        comments: values.comments,
      });
      message.success('Evaluation submitted successfully');
      navigate(`/evaluator/dashboard`);
    } catch {
      message.error('Failed to submit evaluation');
    } finally {
      setSubmitting(false);
    }
  };

  if (!tenderId || !bidId) {
    return (
      <Card>
        <Alert
          type="info"
          message="No tender or bid selected"
          description="Please navigate to this page from the evaluator dashboard by selecting a tender and bid to evaluate."
          showIcon
        />
        <Button type="link" onClick={() => navigate('/evaluator/dashboard')}>
          Go to Dashboard
        </Button>
      </Card>
    );
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '24px 16px' }}>
      <Title level={3}>Bid Evaluation</Title>

      {/* Tender & Bid Summary */}
      {tender && bid && (
        <Card style={{ marginBottom: 24 }}>
          <Descriptions title="Evaluation Context" bordered size="small" column={2}>
            <Descriptions.Item label="Tender">{tender.title}</Descriptions.Item>
            <Descriptions.Item label="Tender ID">{tender.id}</Descriptions.Item>
            <Descriptions.Item label="Bidder">{bid.tendererName ?? `Bidder #${bid.tendererId}`}</Descriptions.Item>
            <Descriptions.Item label="Bid ID">{bid.id}</Descriptions.Item>
            <Descriptions.Item label="Total Price">{bid.totalPrice?.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color="blue">{bid.status}</Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      {/* Conflict of Interest Declaration */}
      <Card
        title="Conflict of Interest Declaration"
        style={{ marginBottom: 24 }}
        extra={conflictDeclared ? <Tag color="green">Declared</Tag> : <Tag color="orange">Pending</Tag>}
      >
        {!conflictDeclared ? (
          <Flex vertical gap={16} style={{ width: '100%' }}>
            <Text>
              Before evaluating, you must declare whether you have any conflict of interest
              with the bidder or the tender subject matter.
            </Text>
            <Checkbox
              checked={hasConflict}
              onChange={(e) => setHasConflict(e.target.checked)}
            >
              I have a conflict of interest
            </Checkbox>
            {hasConflict && (
              <TextArea
                rows={3}
                placeholder="Describe the nature of the conflict of interest..."
                value={conflictDescription}
                onChange={(e) => setConflictDescription(e.target.value)}
              />
            )}
            <Button
              type="primary"
              onClick={handleDeclareConflict}
              loading={declaringConflict}
            >
              Submit Declaration
            </Button>
          </Flex>
        ) : hasConflict ? (
          <Alert
            type="error"
            message="Conflict of Interest Declared"
            description="You have declared a conflict of interest. You are not permitted to evaluate this bid."
            showIcon
          />
        ) : (
          <Alert
            type="success"
            message="No Conflict of Interest"
            description="You have declared no conflict of interest. You may proceed with the evaluation."
            showIcon
          />
        )}
      </Card>

      {/* Scoring Interface */}
      {conflictDeclared && !hasConflict && (
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          {/* Real-time overall score */}
          <Card style={{ marginBottom: 24, textAlign: 'center' }}>
            <Title level={4} style={{ margin: 0 }}>
              Overall Weighted Score: {weightedScore}%
            </Title>
            <Text type="secondary">
              Based on {Object.keys(scores).length} / {activeCriteria.length} criteria scored
            </Text>
          </Card>

          {activeCriteria.map((criteria: TenderCriteria) => (
            <Card
              key={criteria.id}
              title={
                <Space>
                  <Text strong>{criteria.name}</Text>
                  <Tag color="blue">Weight: {criteria.weight}</Tag>
                  <Tag>{criteria.type}</Tag>
                </Space>
              }
              style={{ marginBottom: 16 }}
              size="small"
            >
              {criteria.description && (
                <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                  {criteria.description}
                </Text>
              )}

              <Form.Item
                name={['criteria', criteria.id, 'score']}
                label="Score (0 - 10)"
                rules={[{ required: true, message: 'Please provide a score' }]}
              >
                <Flex vertical gap={8} style={{ width: '100%' }}>
                  <Slider
                    min={0}
                    max={10}
                    step={0.5}
                    value={scores[criteria.id] ?? 0}
                    onChange={(val) => handleScoreChange(criteria.id, val)}
                    marks={{ 0: '0', 2.5: '2.5', 5: '5', 7.5: '7.5', 10: '10' }}
                  />
                  <InputNumber
                    min={0}
                    max={10}
                    step={0.5}
                    value={scores[criteria.id] ?? 0}
                    onChange={(val) => handleScoreChange(criteria.id, val ?? 0)}
                    style={{ width: 120 }}
                  />
                </Flex>
              </Form.Item>

              <Form.Item
                name={['criteria', criteria.id, 'justification']}
                label="Justification"
                rules={[{ required: true, message: 'Please provide a justification for this score' }]}
              >
                <TextArea rows={2} placeholder="Explain the reasoning for this score..." />
              </Form.Item>
            </Card>
          ))}

          <Divider />

          <Form.Item name="comments" label="Overall Comments">
            <TextArea rows={4} placeholder="Any additional comments about this evaluation..." />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
             
              block
              loading={submitting}
            >
              Submit Evaluation
            </Button>
          </Form.Item>
        </Form>
      )}
    </div>
  );
}
