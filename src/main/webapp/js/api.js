/**
 * ContractIQ API Client
 * Wraps Fetch API calls with standard error handling and base URL discovery.
 */
const API = (() => {
  // Compute context path dynamically (e.g. /ContractIQ or /)
  const getContextPath = () => {
    const path = window.location.pathname;
    const slashIdx = path.indexOf('/', 1);
    return slashIdx > 0 ? path.substring(0, slashIdx) : '';
  };

  const BASE_URL = getContextPath() + '/api';

  async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const defaultHeaders = {
      'Accept': 'application/json'
    };

    if (!(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
    }

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const json = await response.json();

      if (!response.ok || !json.success) {
        const errorMsg = json.message || json.error || `HTTP error ${response.status}`;
        throw new Error(errorMsg);
      }

      return json.data;
    } catch (err) {
      console.error(`API Error on ${url}:`, err);
      throw err;
    }
  }

  return {
    getHealth: () => request('/health'),
    getDashboardStats: () => request('/dashboard/stats'),
    
    getContracts: (status = '', search = '') => {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      if (search) params.append('search', search);
      const q = params.toString();
      return request(`/contracts${q ? '?' + q : ''}`);
    },

    getContract: (id) => request(`/contracts/${id}`),

    deleteContract: (id) => request(`/contracts/${id}`, { method: 'DELETE' }),

    uploadContract: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      return request('/contracts/upload', {
        method: 'POST',
        body: formData
      });
    },

    getObligations: (status = '', category = '', search = '') => {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      if (category) params.append('category', category);
      if (search) params.append('search', search);
      const q = params.toString();
      return request(`/obligations${q ? '?' + q : ''}`);
    },

    getContractObligations: (contractId) => request(`/contracts/${contractId}/obligations`),

    updateObligation: (id, updateData) => request(`/obligations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(updateData)
    }),

    approveObligation: (id, reviewerName = 'Legal Counsel', comment = '') => request(`/obligations/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ reviewerName, comment })
    }),

    rejectObligation: (id, reviewerName = 'Legal Counsel', comment = '') => request(`/obligations/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reviewerName, comment })
    }),

    getRenewals: (limit = 50) => request(`/renewals?limit=${limit}`),

    getActivity: (limit = 50, contractId = null) => {
      let q = `?limit=${limit}`;
      if (contractId) q += `&contractId=${contractId}`;
      return request(`/activity${q}`);
    }
  };
})();
