import { describe, it, expect } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToastProvider, useToast } from '../../components/Toast';

function TestConsumer() {
  const { success, error, info, warning } = useToast();
  return (
    <div>
      <button onClick={() => success('Success message')}>Success</button>
      <button onClick={() => error('Error message')}>Error</button>
      <button onClick={() => info('Info message')}>Info</button>
      <button onClick={() => warning('Warning message')}>Warning</button>
    </div>
  );
}

describe('Toast', () => {
  it('renders children', () => {
    render(
      <ToastProvider>
        <div>Content</div>
      </ToastProvider>,
    );
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('shows success toast', async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>,
    );
    await userEvent.click(screen.getByText('Success'));
    expect(screen.getByText('Success message')).toBeInTheDocument();
  });

  it('shows error toast', async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>,
    );
    await userEvent.click(screen.getByText('Error'));
    expect(screen.getByText('Error message')).toBeInTheDocument();
  });

  it('auto-dismisses after timeout', async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>,
    );
    fireEvent.click(screen.getByText('Info'));
    expect(screen.getByText('Info message')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('Info message')).not.toBeInTheDocument();
    }, { timeout: 8000 });
  }, 10000);

  it('dismisses on click', async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>,
    );
    await userEvent.click(screen.getByText('Warning'));
    const toast = screen.getByText('Warning message');
    await userEvent.click(toast);
    await waitFor(() => {
      expect(screen.queryByText('Warning message')).not.toBeInTheDocument();
    });
  });
});
