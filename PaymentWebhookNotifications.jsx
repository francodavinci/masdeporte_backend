import React, { useEffect, useState } from 'react';
import { usePaymentWebhook } from './frontend-webhook-integration';
import './PaymentWebhookNotifications.css';

const PaymentWebhookNotifications = ({ preferenceId, onAppointmentCreated }) => {
  const { paymentStatus, appointmentCreated, loading, error } = usePaymentWebhook(preferenceId);
  const [showNotification, setShowNotification] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState('');

  useEffect(() => {
    if (paymentStatus === 'approved' && appointmentCreated) {
      setNotificationMessage('¡Pago exitoso! Tu reserva ha sido confirmada automáticamente.');
      setShowNotification(true);
      
      // Notificar al componente padre
      if (onAppointmentCreated) {
        onAppointmentCreated();
      }

      // Ocultar notificación después de 5 segundos
      setTimeout(() => {
        setShowNotification(false);
      }, 5000);
    } else if (paymentStatus === 'rejected') {
      setNotificationMessage('El pago fue rechazado. Inténtalo nuevamente.');
      setShowNotification(true);
      
      setTimeout(() => {
        setShowNotification(false);
      }, 5000);
    } else if (paymentStatus === 'cancelled') {
      setNotificationMessage('El pago fue cancelado.');
      setShowNotification(true);
      
      setTimeout(() => {
        setShowNotification(false);
      }, 5000);
    }
  }, [paymentStatus, appointmentCreated, onAppointmentCreated]);

  if (loading) {
    return (
      <div className="webhook-status">
        <div className="status-indicator loading">
          <div className="spinner"></div>
          <span>Verificando estado del pago...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="webhook-status">
        <div className="status-indicator error">
          <i className="fas fa-exclamation-triangle"></i>
          <span>Error: {error}</span>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="webhook-status">
        <div className={`status-indicator ${paymentStatus}`}>
          {paymentStatus === 'pending' && (
            <>
              <i className="fas fa-clock"></i>
              <span>Pago pendiente</span>
            </>
          )}
          {paymentStatus === 'approved' && !appointmentCreated && (
            <>
              <i className="fas fa-check"></i>
              <span>Pago aprobado - Creando reserva...</span>
            </>
          )}
          {paymentStatus === 'approved' && appointmentCreated && (
            <>
              <i className="fas fa-check-circle"></i>
              <span>¡Reserva confirmada!</span>
            </>
          )}
          {paymentStatus === 'rejected' && (
            <>
              <i className="fas fa-times"></i>
              <span>Pago rechazado</span>
            </>
          )}
          {paymentStatus === 'cancelled' && (
            <>
              <i className="fas fa-ban"></i>
              <span>Pago cancelado</span>
            </>
          )}
        </div>
      </div>

      {showNotification && (
        <div className={`webhook-notification ${paymentStatus}`}>
          <div className="notification-content">
            <i className={`fas ${paymentStatus === 'approved' ? 'fa-check-circle' : 'fa-exclamation-triangle'}`}></i>
            <span>{notificationMessage}</span>
            <button 
              className="notification-close"
              onClick={() => setShowNotification(false)}
            >
              <i className="fas fa-times"></i>
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export default PaymentWebhookNotifications; 