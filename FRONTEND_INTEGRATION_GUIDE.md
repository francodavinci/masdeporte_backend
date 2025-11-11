# Guía de Integración Frontend - Sistema de Pagos Agendalo

## Mejoras al Servicio de Pagos Actual

### 1. Actualizar el `paymentsService`

```javascript
const API_URL = "http://localhost:8080/api/mercadopago";

export const paymentsService = {
    // Métodos existentes
    getStatus: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/oauth/status`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener el estado de Mercado Pago");
        const data = await response.json();
        return data.data; // Extraer data del ApiResponse
    },

    disconnect: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/oauth/disconnect`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo desvincular Mercado Pago");
        return true;
    },

    createPaymentPreference: async (paymentData) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(paymentData)
        });
        if (!response.ok) throw new Error("No se pudo crear la preferencia de pago");
        const data = await response.json();
        return data.data; // Extraer data del ApiResponse
    },

    // Nuevos métodos
    getUserPreferences: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener las preferencias");
        const data = await response.json();
        return data.data;
    },

    getPreferenceById: async (preferenceId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences/${preferenceId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener la preferencia");
        const data = await response.json();
        return data.data;
    },

    getPendingPayments: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/appointments/pending-payment`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener los pagos pendientes");
        const data = await response.json();
        return data.data;
    },

    cancelPreference: async (preferenceId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences/${preferenceId}/cancel`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo cancelar la preferencia");
        return true;
    },

    getPaymentStatus: async (paymentId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/payment/${paymentId}/status`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener el estado del pago");
        const data = await response.json();
        return data.data;
    },

    getStatistics: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/statistics`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener las estadísticas");
        const data = await response.json();
        return data.data;
    }
};
```

## 2. Componente Mejorado de Pagos

```jsx
import React, { useEffect, useState } from "react";
import { paymentsService } from "../../services/paymentsService";
import "./PaymentsPage.css";

const CLIENT_ID = "1984353412837350";
const REDIRECT_URI = "https://j-electronics-bizrate-silent.trycloudflare.com/dashboard/pagos/callback";

const PaymentsPage = () => {
  const [mpStatus, setMpStatus] = useState({ isConnected: false, status: "not_connected" });
  const [preferences, setPreferences] = useState([]);
  const [pendingPayments, setPendingPayments] = useState([]);
  const [statistics, setStatistics] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("mp") === "success") {
      setLoading(true);
      loadData();
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  const loadData = async () => {
    try {
      const [statusData, preferencesData, pendingData, statsData] = await Promise.all([
        paymentsService.getStatus(),
        paymentsService.getUserPreferences(),
        paymentsService.getPendingPayments(),
        paymentsService.getStatistics()
      ]);

      setMpStatus(statusData);
      setPreferences(preferencesData);
      setPendingPayments(pendingData);
      setStatistics(statsData);
    } catch (error) {
      console.error("Error cargando datos:", error);
      setMpStatus({ isConnected: false, status: "not_connected" });
    } finally {
      setLoading(false);
    }
  };

  const handleConnectMercadoPago = () => {
    const authUrl = `https://auth.mercadopago.com.ar/authorization?client_id=${CLIENT_ID}&response_type=code&platform_id=mp&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`;
    window.location.href = authUrl;
  };

  const handleDisconnect = async () => {
    setLoading(true);
    try {
      await paymentsService.disconnect();
      setMpStatus({ isConnected: false, status: "not_connected" });
      await loadData(); // Recargar datos
    } catch (error) {
      console.error("Error desconectando:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelPreference = async (preferenceId) => {
    try {
      await paymentsService.cancelPreference(preferenceId);
      await loadData(); // Recargar datos
      alert("Preferencia cancelada exitosamente");
    } catch (error) {
      console.error("Error cancelando preferencia:", error);
      alert("Error al cancelar la preferencia");
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('es-AR');
  };

  const getStatusBadge = (status) => {
    const statusConfig = {
      pending: { class: 'pending', text: 'Pendiente' },
      approved: { class: 'approved', text: 'Aprobado' },
      cancelled: { class: 'cancelled', text: 'Cancelado' },
      rejected: { class: 'rejected', text: 'Rechazado' }
    };
    const config = statusConfig[status] || { class: 'unknown', text: 'Desconocido' };
    return <span className={`status-badge ${config.class}`}>{config.text}</span>;
  };

  if (loading) {
    return (
      <div className="payments-page-container">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Cargando información de pagos...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="payments-page-container">
      <div className="payments-header">
        <h2>Pagos</h2>
        <div className="tab-navigation">
          <button 
            className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            Resumen
          </button>
          <button 
            className={`tab-btn ${activeTab === 'preferences' ? 'active' : ''}`}
            onClick={() => setActiveTab('preferences')}
          >
            Preferencias
          </button>
          <button 
            className={`tab-btn ${activeTab === 'pending' ? 'active' : ''}`}
            onClick={() => setActiveTab('pending')}
          >
            Pendientes
          </button>
        </div>
      </div>

      <div className="payments-main">
        {/* Sección de Conexión Mercado Pago */}
        <div className={`mercadopago-section ${mpStatus.isConnected ? "connected" : ""}`}>
          <div className="mp-info">
            <div className="mp-details">
              <h3>Mercado Pago</h3>
              {mpStatus.isConnected ? (
                <>
                  <p className="success-message">
                    ¡Tu cuenta está conectada y lista para recibir pagos!
                  </p>
                  <span className="mp-status-badge active">Activa</span>
                </>
              ) : (
                <p>
                  Conecta tu cuenta de Mercado Pago para recibir pagos online.
                </p>
              )}
            </div>
          </div>
          {!mpStatus.isConnected && (
            <button
              className="mp-connect-btn"
              onClick={handleConnectMercadoPago}
              disabled={loading}
            >
              <i className="fas fa-link"></i> Conectar con Mercado Pago
            </button>
          )}
          {mpStatus.isConnected && (
            <button
              className="mp-connect-btn disconnect"
              onClick={handleDisconnect}
              disabled={loading}
            >
              <i className="fas fa-unlink"></i> Desvincular cuenta
            </button>
          )}
        </div>

        {/* Tab de Resumen */}
        {activeTab === 'overview' && (
          <div className="overview-section">
            <div className="statistics-grid">
              <div className="stat-card">
                <h4>Total de Preferencias</h4>
                <p className="stat-number">{statistics.totalPreferences || 0}</p>
              </div>
              <div className="stat-card">
                <h4>Pagos Aprobados</h4>
                <p className="stat-number approved">{statistics.approvedCount || 0}</p>
              </div>
              <div className="stat-card">
                <h4>Pagos Pendientes</h4>
                <p className="stat-number pending">{statistics.pendingCount || 0}</p>
              </div>
              <div className="stat-card">
                <h4>Ingresos Totales</h4>
                <p className="stat-number earnings">{formatCurrency(statistics.totalEarnings || 0)}</p>
              </div>
            </div>
          </div>
        )}

        {/* Tab de Preferencias */}
        {activeTab === 'preferences' && (
          <div className="preferences-section">
            <h3>Historial de Preferencias de Pago</h3>
            {preferences.length === 0 ? (
              <p className="empty-state">No hay preferencias de pago registradas.</p>
            ) : (
              <div className="preferences-list">
                {preferences.map((pref) => (
                  <div key={pref.id} className="preference-card">
                    <div className="preference-header">
                      <h4>{pref.service?.name || 'Servicio'}</h4>
                      {getStatusBadge(pref.status)}
                    </div>
                    <div className="preference-details">
                      <p><strong>Monto:</strong> {formatCurrency(pref.amount)}</p>
                      <p><strong>Fecha:</strong> {formatDate(pref.startTime)}</p>
                      <p><strong>Empresa:</strong> {pref.company?.name || 'N/A'}</p>
                      {pref.notes && <p><strong>Notas:</strong> {pref.notes}</p>}
                    </div>
                    {pref.status === 'pending' && (
                      <button
                        className="cancel-btn"
                        onClick={() => handleCancelPreference(pref.preferenceId)}
                      >
                        Cancelar
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab de Pendientes */}
        {activeTab === 'pending' && (
          <div className="pending-section">
            <h3>Pagos Pendientes</h3>
            {pendingPayments.length === 0 ? (
              <p className="empty-state">No hay pagos pendientes.</p>
            ) : (
              <div className="pending-list">
                {pendingPayments.map((payment) => (
                  <div key={payment.id} className="pending-card">
                    <div className="pending-header">
                      <h4>{payment.service?.name || 'Servicio'}</h4>
                      <span className="pending-badge">Pendiente de Pago</span>
                    </div>
                    <div className="pending-details">
                      <p><strong>Monto:</strong> {formatCurrency(payment.amount)}</p>
                      <p><strong>Fecha Programada:</strong> {formatDate(payment.startTime)}</p>
                      <p><strong>Empresa:</strong> {payment.company?.name || 'N/A'}</p>
                    </div>
                    <div className="pending-actions">
                      <button
                        className="cancel-btn"
                        onClick={() => handleCancelPreference(payment.preferenceId)}
                      >
                        Cancelar Reserva
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default PaymentsPage;
```

## 3. Estilos CSS Mejorados

```css
/* PaymentsPage.css */
.payments-page-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.payments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 15px;
}

.tab-navigation {
  display: flex;
  gap: 10px;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: #007bff;
  color: white;
}

.tab-btn:hover {
  background: #0056b3;
  color: white;
}

.mercadopago-section {
  background: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  border-left: 4px solid #f0f0f0;
}

.mercadopago-section.connected {
  border-left-color: #28a745;
}

.mp-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.mp-status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.mp-status-badge.active {
  background: #d4edda;
  color: #155724;
}

.mp-connect-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  background: #007bff;
  color: white;
}

.mp-connect-btn:hover {
  background: #0056b3;
}

.mp-connect-btn.disconnect {
  background: #dc3545;
}

.mp-connect-btn.disconnect:hover {
  background: #c82333;
}

/* Estadísticas */
.statistics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  text-align: center;
}

.stat-number {
  font-size: 2em;
  font-weight: bold;
  margin: 10px 0;
}

.stat-number.approved {
  color: #28a745;
}

.stat-number.pending {
  color: #ffc107;
}

.stat-number.earnings {
  color: #007bff;
}

/* Listas */
.preferences-list,
.pending-list {
  display: grid;
  gap: 20px;
}

.preference-card,
.pending-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  border-left: 4px solid #e0e0e0;
}

.preference-header,
.pending-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

.status-badge.approved {
  background: #d4edda;
  color: #155724;
}

.status-badge.cancelled {
  background: #f8d7da;
  color: #721c24;
}

.status-badge.rejected {
  background: #f8d7da;
  color: #721c24;
}

.pending-badge {
  background: #fff3cd;
  color: #856404;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.preference-details,
.pending-details {
  margin-bottom: 15px;
}

.preference-details p,
.pending-details p {
  margin: 5px 0;
  color: #666;
}

.cancel-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn:hover {
  background: #c82333;
}

.empty-state {
  text-align: center;
  color: #666;
  font-style: italic;
  padding: 40px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
```

## 4. Componente de Creación de Reserva con Pago

```jsx
// CreateAppointmentWithPayment.jsx
import React, { useState } from 'react';
import { paymentsService } from '../../services/paymentsService';

const CreateAppointmentWithPayment = ({ service, onSuccess, onCancel }) => {
  const [formData, setFormData] = useState({
    startTime: '',
    notes: '',
    userEmail: localStorage.getItem('userEmail') || ''
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const paymentData = {
        title: service.name,
        description: service.description,
        amount: service.price,
        quantity: 1,
        currency: "ARS",
        serviceId: service.id,
        userId: service.company.owner.id,
        startTime: formData.startTime,
        notes: formData.notes,
        userEmail: formData.userEmail
      };

      const preference = await paymentsService.createPaymentPreference(paymentData);
      
      // Redirigir a Mercado Pago
      if (preference.init_point) {
        window.location.href = preference.init_point;
      } else {
        throw new Error('No se pudo obtener el enlace de pago');
      }
    } catch (error) {
      console.error('Error creando preferencia:', error);
      alert('Error al crear la preferencia de pago');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="appointment-payment-modal">
      <div className="modal-content">
        <h3>Reservar {service.name}</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Fecha y Hora:</label>
            <input
              type="datetime-local"
              value={formData.startTime}
              onChange={(e) => setFormData({...formData, startTime: e.target.value})}
              required
            />
          </div>
          
          <div className="form-group">
            <label>Notas (opcional):</label>
            <textarea
              value={formData.notes}
              onChange={(e) => setFormData({...formData, notes: e.target.value})}
              placeholder="Agregar notas adicionales..."
            />
          </div>

          <div className="payment-summary">
            <h4>Resumen del Pago</h4>
            <p><strong>Servicio:</strong> {service.name}</p>
            <p><strong>Precio:</strong> ${service.price}</p>
            <p><strong>Duración:</strong> {service.durationMinutes} minutos</p>
          </div>

          <div className="modal-actions">
            <button type="button" onClick={onCancel} disabled={loading}>
              Cancelar
            </button>
            <button type="submit" disabled={loading}>
              {loading ? 'Procesando...' : 'Pagar y Reservar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateAppointmentWithPayment;
```

## 5. Hook Personalizado para Estado de Pagos

```javascript
// usePaymentStatus.js
import { useState, useEffect } from 'react';
import { paymentsService } from '../services/paymentsService';

export const usePaymentStatus = (paymentId, interval = 5000) => {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!paymentId) return;

    const checkStatus = async () => {
      try {
        const paymentInfo = await paymentsService.getPaymentStatus(paymentId);
        setStatus(paymentInfo);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    // Verificar inmediatamente
    checkStatus();

    // Configurar polling
    const intervalId = setInterval(checkStatus, interval);

    return () => clearInterval(intervalId);
  }, [paymentId, interval]);

  return { status, loading, error };
};
```

## 6. Página de Callback de Pago

```jsx
// PaymentCallback.jsx
import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { paymentsService } from '../services/paymentsService';

const PaymentCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('processing');
  const [message, setMessage] = useState('Procesando pago...');

  useEffect(() => {
    const paymentId = searchParams.get('payment_id');
    const preferenceId = searchParams.get('preference_id');
    const status = searchParams.get('status');

    if (paymentId && status) {
      handlePaymentCallback(paymentId, status);
    } else {
      setStatus('error');
      setMessage('Información de pago incompleta');
    }
  }, [searchParams]);

  const handlePaymentCallback = async (paymentId, status) => {
    try {
      if (status === 'approved') {
        setMessage('¡Pago exitoso! Tu reserva ha sido confirmada.');
        setStatus('success');
        
        // Opcional: verificar estado del pago
        const paymentInfo = await paymentsService.getPaymentStatus(paymentId);
        console.log('Payment info:', paymentInfo);
        
        // Redirigir después de 3 segundos
        setTimeout(() => {
          navigate('/dashboard/appointments');
        }, 3000);
      } else if (status === 'pending') {
        setMessage('Pago pendiente. Te notificaremos cuando se confirme.');
        setStatus('pending');
      } else {
        setMessage('El pago no pudo ser procesado. Inténtalo nuevamente.');
        setStatus('error');
      }
    } catch (error) {
      console.error('Error procesando callback:', error);
      setStatus('error');
      setMessage('Error procesando el pago');
    }
  };

  return (
    <div className="payment-callback">
      <div className={`callback-card ${status}`}>
        <div className="callback-icon">
          {status === 'success' && <i className="fas fa-check-circle"></i>}
          {status === 'pending' && <i className="fas fa-clock"></i>}
          {status === 'error' && <i className="fas fa-times-circle"></i>}
          {status === 'processing' && <div className="spinner"></div>}
        </div>
        <h2>{message}</h2>
        {status === 'success' && (
          <p>Serás redirigido automáticamente a tus reservas...</p>
        )}
        {status === 'error' && (
          <button onClick={() => navigate('/dashboard')}>
            Volver al Dashboard
          </button>
        )}
      </div>
    </div>
  );
};

export default PaymentCallback;
```

## Resumen de Mejoras

### ✅ **Funcionalidades Agregadas:**
1. **Dashboard completo** con estadísticas y tabs
2. **Gestión de preferencias** con cancelación
3. **Pagos pendientes** con acciones
4. **Estado en tiempo real** de pagos
5. **Página de callback** para confirmaciones
6. **Componente de reserva con pago** integrado

### ✅ **Mejoras de UX:**
1. **Loading states** apropiados
2. **Manejo de errores** robusto
3. **Formateo de moneda** y fechas
4. **Badges de estado** visuales
5. **Responsive design** con CSS Grid

### ✅ **Integración Completa:**
1. **Todos los endpoints** del backend utilizados
2. **Manejo de ApiResponse** correcto
3. **Autenticación** con tokens
4. **Redirección** a Mercado Pago
5. **Webhooks** procesados automáticamente

El frontend ahora está completamente integrado con el sistema de pagos y puede manejar todo el flujo desde la creación de reservas hasta la confirmación automática después del pago exitoso. 