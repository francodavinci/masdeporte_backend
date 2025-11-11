import React, { useEffect, useState } from "react";
import { paymentsService, useMultiplePaymentWebhooks } from "./frontend-webhook-integration";
import PaymentWebhookNotifications from "./PaymentWebhookNotifications";
import "./PaymentsPage.css";

const CLIENT_ID = "1984353412837350";
const REDIRECT_URI = "https://tamil-jungle-valuation-helena.trycloudflare.com/dashboard/pagos/callback";

const PaymentsPageWithWebhooks = () => {
  const [mpStatus, setMpStatus] = useState({ isConnected: false, status: "not_connected" });
  const [preferences, setPreferences] = useState([]);
  const [pendingPayments, setPendingPayments] = useState([]);
  const [statistics, setStatistics] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');
  const [currentPreferenceId, setCurrentPreferenceId] = useState(null);

  // Obtener IDs de preferencias pendientes para monitoreo
  const pendingPreferenceIds = pendingPayments.map(p => p.preferenceId);
  
  // Hook para monitorear múltiples pagos en tiempo real
  const { payments: realTimePayments, loading: webhookLoading } = useMultiplePaymentWebhooks(
    pendingPreferenceIds,
    3000 // Verificar cada 3 segundos
  );

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

  // Efecto para actualizar datos cuando cambian los pagos en tiempo real
  useEffect(() => {
    if (Object.keys(realTimePayments).length > 0) {
      updatePaymentsFromWebhooks();
    }
  }, [realTimePayments]);

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

  const updatePaymentsFromWebhooks = () => {
    // Actualizar preferencias con datos en tiempo real
    setPreferences(prevPreferences => 
      prevPreferences.map(pref => {
        const realTimeData = realTimePayments[pref.preferenceId];
        if (realTimeData && !realTimeData.error) {
          return { ...pref, ...realTimeData };
        }
        return pref;
      })
    );

    // Actualizar pagos pendientes
    setPendingPayments(prevPending => 
      prevPending.filter(payment => {
        const realTimeData = realTimePayments[payment.preferenceId];
        if (realTimeData && !realTimeData.error) {
          // Si el pago ya no está pendiente, removerlo de la lista
          return realTimeData.status === 'pending';
        }
        return true;
      })
    );

    // Recargar estadísticas si hay cambios
    if (Object.keys(realTimePayments).length > 0) {
      setTimeout(() => {
        loadData();
      }, 1000);
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
      await loadData();
    } catch (error) {
      console.error("Error desconectando:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelPreference = async (preferenceId) => {
    try {
      await paymentsService.cancelPreference(preferenceId);
      await loadData();
      alert("Preferencia cancelada exitosamente");
    } catch (error) {
      console.error("Error cancelando preferencia:", error);
      alert("Error al cancelar la preferencia");
    }
  };

  const handleAppointmentCreated = () => {
    // Recargar datos cuando se crea una reserva
    loadData();
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
            Pendientes {pendingPayments.length > 0 && `(${pendingPayments.length})`}
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

        {/* Indicador de webhooks en tiempo real */}
        {webhookLoading && pendingPayments.length > 0 && (
          <div className="webhook-indicator">
            <div className="spinner"></div>
            <span>Monitoreando pagos en tiempo real...</span>
          </div>
        )}

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
                    
                    {/* Webhook notifications para preferencias pendientes */}
                    {pref.status === 'pending' && (
                      <PaymentWebhookNotifications 
                        preferenceId={pref.preferenceId}
                        onAppointmentCreated={handleAppointmentCreated}
                      />
                    )}
                    
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
                    
                    {/* Webhook notifications para pagos pendientes */}
                    <PaymentWebhookNotifications 
                      preferenceId={payment.preferenceId}
                      onAppointmentCreated={handleAppointmentCreated}
                    />
                    
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

export default PaymentsPageWithWebhooks; 