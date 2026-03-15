import { Form, Input, Button, Card, Typography, Select, message, Space, Divider } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { UserRole } from '../../types';
import { useState } from 'react';

const { Title, Text } = Typography;

export default function RegisterPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm();
  const [role, setRole] = useState<UserRole>('TENDERER');

  const onFinish = async (values: Record<string, unknown>) => {
    try {
      const payload = {
        username: values.username as string,
        email: values.email as string,
        password: values.password as string,
        role: values.role as UserRole,
        ...(values.role === 'TENDERER' && {
          organization: {
            name: values.orgName as string,
            registrationNumber: values.registrationNumber as string,
            contactPerson: values.contactPerson as string,
            phone: values.phone as string,
            email: values.orgEmail as string,
            organizationType: values.organizationType as 'GOVERNMENT' | 'PRIVATE' | 'NGO',
          },
        }),
      };
      const { data } = await authApi.register(payload);
      login(data.token, data.userId, data.username, data.role);
      message.success('Registration successful');
      navigate('/dashboard');
    } catch {
      message.error('Registration failed. Please try again.');
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #1a365d 0%, #2d5f8a 100%)', padding: 24 }}>
      <Card style={{ width: 520, borderRadius: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Title level={3} style={{ margin: 0, color: '#1a365d' }}>Create Account</Title>
            <Text type="secondary">E-Government Procurement System</Text>
          </div>
          <Form form={form} onFinish={onFinish} layout="vertical" initialValues={{ role: 'TENDERER' }}>
            <Form.Item name="username" label="Username" rules={[{ required: true }, { min: 3, max: 50 }]}>
              <Input placeholder="Choose a username" />
            </Form.Item>
            <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
              <Input placeholder="your@email.com" />
            </Form.Item>
            <Form.Item name="password" label="Password" rules={[{ required: true, min: 6 }]}>
              <Input.Password placeholder="Minimum 6 characters" />
            </Form.Item>
            <Form.Item name="role" label="Role" rules={[{ required: true }]}>
              <Select onChange={(v) => setRole(v)} options={[
                { value: 'TENDERER', label: 'Vendor / Bidder' },
                { value: 'TENDEREE', label: 'Government Officer' },
                { value: 'EVALUATOR', label: 'Evaluator' },
                { value: 'COMMITTEE', label: 'Committee Member' },
              ]} />
            </Form.Item>

            {role === 'TENDERER' && (
              <>
                <Divider>Organization Details</Divider>
                <Form.Item name="orgName" label="Organization Name" rules={[{ required: true }]}>
                  <Input placeholder="Company name" />
                </Form.Item>
                <Form.Item name="registrationNumber" label="Registration Number" rules={[{ required: true }]}>
                  <Input placeholder="Business registration number" />
                </Form.Item>
                <Form.Item name="organizationType" label="Organization Type" rules={[{ required: true }]}>
                  <Select options={[
                    { value: 'PRIVATE', label: 'Private Company' },
                    { value: 'NGO', label: 'NGO' },
                    { value: 'GOVERNMENT', label: 'Government Entity' },
                  ]} />
                </Form.Item>
                <Form.Item name="contactPerson" label="Contact Person">
                  <Input placeholder="Full name" />
                </Form.Item>
                <Form.Item name="phone" label="Phone">
                  <Input placeholder="+251..." />
                </Form.Item>
                <Form.Item name="orgEmail" label="Organization Email">
                  <Input placeholder="org@company.com" />
                </Form.Item>
              </>
            )}

            <Form.Item>
              <Button type="primary" htmlType="submit" block style={{ background: '#1a365d' }}>Register</Button>
            </Form.Item>
          </Form>
          <div style={{ textAlign: 'center' }}>
            <Text>Already have an account? <Link to="/login">Sign in</Link></Text>
          </div>
        </Space>
      </Card>
    </div>
  );
}
