import { Form, Input, Button, Card, Typography, message, Flex } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import type { LoginRequest } from '../../types';

const { Title, Text } = Typography;

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [form] = Form.useForm();

  const onFinish = async (values: LoginRequest) => {
    try {
      const { data } = await authApi.login(values);
      login(data.token, data.userId, data.username, data.role);
      message.success('Login successful');
      navigate('/dashboard');
    } catch {
      message.error('Invalid credentials');
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #1a365d 0%, #2d5f8a 100%)' }}>
      <Card style={{ width: 420, borderRadius: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        <Flex vertical gap={24} style={{ width: '100%', textAlign: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0, color: '#1a365d' }}>E-Government Tendering</Title>
            <Text type="secondary">Procurement Management System</Text>
          </div>
          <Form form={form} onFinish={onFinish} layout="vertical">
            <Form.Item name="usernameOrEmail" rules={[{ required: true, message: 'Username or email is required' }]}>
              <Input prefix={<UserOutlined />} placeholder="Username or Email" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: 'Password is required' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="Password" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block style={{ background: '#1a365d' }}>
                Sign In
              </Button>
            </Form.Item>
          </Form>
          <Text>Don't have an account? <Link to="/register">Register here</Link></Text>
        </Flex>
      </Card>
    </div>
  );
}
