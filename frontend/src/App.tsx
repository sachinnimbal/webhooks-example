import { useState, useEffect } from "react";
import {
  Package,
  Webhook,
  Activity,
  AlertCircle,
  CheckCircle,
  Clock,
  XCircle,
  RefreshCw,
  Plus,
  Settings,
  TrendingDown,
  DollarSign,
  Archive,
  X,
  Edit,
  Trash2,
  Bell,
} from "lucide-react";
import toast, { Toaster } from "react-hot-toast";
const API_BASE_URL = "http://localhost:8080/api";

const App = () => {
  const [activeTab, setActiveTab] = useState("products");
  const [products, setProducts] = useState([]);
  const [webhookSubscriptions, setWebhookSubscriptions] = useState([]);
  const [webhookDeliveries, setWebhookDeliveries] = useState([]);
  const [prevDeliveryCount, setPrevDeliveryCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [stats, setStats] = useState({
    totalDeliveries: 0,
    successfulDeliveries: 0,
    failedDeliveries: 0,
    retrying: 0,
    successRate: 0,
  });
  const [loading, setLoading] = useState(false);
  const [showAddProduct, setShowAddProduct] = useState(false);
  const [showAddSubscription, setShowAddSubscription] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [error, setError] = useState(null);

  // Initial product form data
  const getInitialProductData = () => ({
    sku: `SKU-${Math.floor(Math.random() * 10000)}`,
    name: "New Product",
    description: "Product description",
    price: 99.99,
    stockQuantity: 100,
    lowStockThreshold: 10,
    status: "ACTIVE",
    category: "Electronics",
    brand: "Brand Name",
    imageUrl: "",
  });

  const [productForm, setProductForm] = useState(getInitialProductData());

  // Initial subscription form data
  const getInitialSubscriptionData = () => ({
    name: "New Webhook",
    webhookUrl: "http://localhost:8080/test/webhook-receiver",
    subscribedEvents: ["product.created", "product.updated"],
  });

  const [subscriptionForm, setSubscriptionForm] = useState(
    getInitialSubscriptionData()
  );

  const availableEvents = [
    "product.created",
    "product.updated",
    "product.deleted",
    "product.stock.updated",
    "product.stock.low",
    "product.price.changed",
    "product.bulk.updated",
  ];

  // Fetch data
  useEffect(() => {
    fetchProducts();
    fetchSubscriptions();
    fetchDeliveries();
    fetchStats();

    const interval = setInterval(() => {
      fetchDeliveries();
      fetchStats();
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const fetchProducts = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/products`);
      const data = await response.json();
      setProducts(data);
    } catch (err) {
      console.error("Error fetching products:", err);
    }
  };

  const fetchSubscriptions = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/webhooks/subscriptions`);
      const data = await response.json();
      setWebhookSubscriptions(data);
    } catch (err) {
      console.error("Error fetching subscriptions:", err);
    }
  };

  const fetchDeliveries = async () => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/webhooks/deliveries?size=5000`
      );
      const data = await response.json();
      setWebhookDeliveries(data);
    } catch (err) {
      console.error("Error fetching deliveries:", err);
    }
  };

  const fetchStats = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/webhooks/stats`);
      const data = await response.json();
      setStats(data);
    } catch (err) {
      console.error("Error fetching stats:", err);
    }
  };

  // Product operations
  const handleCreateProduct = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_BASE_URL}/products`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(productForm),
      });

      if (!response.ok) throw new Error("Failed to create product");

      await fetchProducts();
      setShowAddProduct(false);
      setProductForm(getInitialProductData());
      toast.success("Product created successfully!");
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProduct = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(
        `${API_BASE_URL}/products/${editingProduct.id}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(productForm),
        }
      );

      if (!response.ok) throw new Error("Failed to update product");

      await fetchProducts();
      setEditingProduct(null);
      setShowAddProduct(false);
      setProductForm(getInitialProductData());
      toast.success("Product updated successfully!");
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteProduct = async (id) => {
    if (!confirm("Are you sure you want to delete this product?")) return;

    try {
      const response = await fetch(`${API_BASE_URL}/products/${id}`, {
        method: "DELETE",
      });

      if (!response.ok) throw new Error("Failed to delete product");

      await fetchProducts();
      toast.success("Product deleted successfully!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  const handleStockUpdate = async (id, quantity, operation) => {
    try {
      const response = await fetch(`${API_BASE_URL}/products/${id}/stock`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity, operation }),
      });

      if (!response.ok) throw new Error("Failed to update stock");

      await fetchProducts();
      toast.success("Stock updated successfully!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  const handlePriceUpdate = async (id, currentPrice) => {
    const discountedPrice = (currentPrice * 0.9).toFixed(2);
    try {
      const response = await fetch(`${API_BASE_URL}/products/${id}/price`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          price: parseFloat(discountedPrice),
          reason: "10% discount applied",
        }),
      });

      if (!response.ok) throw new Error("Failed to update price");

      await fetchProducts();
      toast.success("Price updated successfully!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  // Subscription operations
  const handleCreateSubscription = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_BASE_URL}/webhooks/subscriptions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...subscriptionForm,
          subscribedEvents: Array.from(subscriptionForm.subscribedEvents),
        }),
      });

      if (!response.ok) throw new Error("Failed to create subscription");

      await fetchSubscriptions();
      setShowAddSubscription(false);
      setSubscriptionForm(getInitialSubscriptionData());
      toast.success("Subscription created successfully!");
    } catch (err) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleSubscription = async (id) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/webhooks/subscriptions/${id}/toggle`,
        {
          method: "PATCH",
        }
      );

      if (!response.ok) throw new Error("Failed to toggle subscription");

      await fetchSubscriptions();
      toast.success("Subscription toggled successfully!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  const handleTestWebhook = async (id) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/webhooks/subscriptions/${id}/test`,
        {
          method: "POST",
        }
      );

      if (!response.ok) throw new Error("Failed to test webhook");

      toast.success("Test webhook sent successfully!");
      await fetchDeliveries();
    } catch (err) {
      toast.error(err.message);
    }
  };

  const handleDeleteSubscription = async (id) => {
    if (!confirm("Are you sure you want to delete this subscription?")) return;

    try {
      const response = await fetch(
        `${API_BASE_URL}/webhooks/subscriptions/${id}`,
        {
          method: "DELETE",
        }
      );

      if (!response.ok) throw new Error("Failed to delete subscription");

      await fetchSubscriptions();
      toast.success("Subscription deleted successfully!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  const handleRetryDelivery = async (deliveryId) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/webhooks/deliveries/${deliveryId}/retry`,
        {
          method: "POST",
        }
      );

      if (!response.ok) throw new Error("Failed to retry delivery");

      await fetchDeliveries();
      toast.success("Delivery retry initiated!");
    } catch (err) {
      toast.error(err.message);
    }
  };

  const getStatusColor = (status) => {
    const statusMap = {
      ACTIVE: "bg-green-100 text-green-800",
      OUT_OF_STOCK: "bg-red-100 text-red-800",
      INACTIVE: "bg-gray-100 text-gray-800",
      DISCONTINUED: "bg-gray-100 text-gray-800",
      DELIVERED: "bg-green-100 text-green-800",
      FAILED: "bg-red-100 text-red-800",
      RETRYING: "bg-yellow-100 text-yellow-800",
      PENDING: "bg-blue-100 text-blue-800",
      EXHAUSTED: "bg-red-100 text-red-800",
    };
    return statusMap[status] || "bg-gray-100 text-gray-800";
  };

  const getStatusIcon = (status) => {
    const iconMap = {
      DELIVERED: <CheckCircle className="w-4 h-4" />,
      FAILED: <XCircle className="w-4 h-4" />,
      RETRYING: <RefreshCw className="w-4 h-4 animate-spin" />,
      PENDING: <Clock className="w-4 h-4" />,
      EXHAUSTED: <XCircle className="w-4 h-4" />,
    };
    return iconMap[status] || null;
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "N/A";
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  };

  const getRelativeTime = (dateTime) => {
    if (!dateTime) return "Never";
    const date = new Date(dateTime);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);

    if (diff < 60) return `${diff} sec ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} hours ago`;
    return `${Math.floor(diff / 86400)} days ago`;
  };

  const openEditProduct = (product) => {
    setEditingProduct(product);
    setProductForm({
      sku: product.sku,
      name: product.name,
      description: product.description || "",
      price: product.price,
      stockQuantity: product.stockQuantity,
      lowStockThreshold: product.lowStockThreshold || 10,
      status: product.status,
      category: product.category || "",
      brand: product.brand || "",
      imageUrl: product.imageUrl || "",
    });
    setShowAddProduct(true);
  };

  const closeProductModal = () => {
    setShowAddProduct(false);
    setEditingProduct(null);
    setProductForm(getInitialProductData());
    setError(null);
  };

  const handleEventToggle = (event) => {
    const events = new Set(subscriptionForm.subscribedEvents);
    if (events.has(event)) {
      events.delete(event);
    } else {
      events.add(event);
    }
    setSubscriptionForm({
      ...subscriptionForm,
      subscribedEvents: Array.from(events),
    });
  };

  // Play notification sound
  const playNotificationSound = (status) => {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);

    // Different tones for different statuses
    if (status === "DELIVERED") {
      oscillator.frequency.value = 800; // Success tone
      oscillator.type = "sine";
    } else if (status === "FAILED") {
      oscillator.frequency.value = 400; // Error tone
      oscillator.type = "square";
    } else {
      oscillator.frequency.value = 600; // Retry tone
      oscillator.type = "triangle";
    }

    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);

    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + 0.3);
  };

  useEffect(() => {
    if (webhookDeliveries.length > prevDeliveryCount && prevDeliveryCount > 0) {
      const latestDelivery = webhookDeliveries[0];
      playNotificationSound(latestDelivery.status);
      if (latestDelivery.status === "DELIVERED") {
        toast.success(`✓ Webhook delivered: ${latestDelivery.eventType}`);
      } else if (latestDelivery.status === "FAILED") {
        toast.error(`✗ Webhook failed: ${latestDelivery.eventType}`);
      } else if (latestDelivery.status === "RETRYING") {
        toast.loading(`⟳ Retrying webhook: ${latestDelivery.eventType}`, {
          duration: 2000,
        });
      }
    }
    setPrevDeliveryCount(webhookDeliveries.length);
  }, [webhookDeliveries, prevDeliveryCount]);

  useEffect(() => {
    setUnreadCount(webhookDeliveries.filter((d) => !d.read).length);
  }, [webhookDeliveries]);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Toast Container */}
      <Toaster
        position="bottom-right"
        reverseOrder={false}
        toastOptions={{
          duration: 3000,
          style: {
            background: "#fff",
            color: "#363636",
          },
          success: {
            duration: 3000,
            iconTheme: {
              primary: "#10B981",
              secondary: "#fff",
            },
          },
          error: {
            duration: 4000,
            iconTheme: {
              primary: "#EF4444",
              secondary: "#fff",
            },
          },
        }}
      />
      {/* Header */}
      <header className="sticky top-0 z-10 bg-white border-b border-gray-200">
        <div className="px-4 mx-auto max-w-7xl sm:px-6 lg:px-8">
          <div className="flex items-center justify-between py-4">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-blue-600 rounded-lg">
                <Package className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">
                  Product Webhook Dashboard
                </h1>
                <p className="text-sm text-gray-500">
                  Real-time inventory & webhook monitoring
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <div className="relative">
                <button
                  onClick={() => setShowNotifications(!showNotifications)}
                  className="relative p-2 text-gray-600 transition-colors rounded-lg hover:bg-gray-100"
                >
                  <Bell className="w-6 h-6" />
                  {unreadCount > 0 && (
                    <span className="absolute flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-red-500 rounded-full -top-1 -right-1">
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </button>

                {showNotifications && (
                  <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-xl border border-gray-200 z-50 max-h-[500px] overflow-y-auto">
                    <div className="flex items-center justify-between p-4 border-b border-gray-200">
                      <h3 className="font-semibold text-gray-900">
                        Webhook Deliveries
                      </h3>
                      <button
                        onClick={() => setShowNotifications(false)}
                        className="text-gray-400 hover:text-gray-600"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                    <div className="divide-y divide-gray-100">
                      {webhookDeliveries.slice(0, 20).map((delivery) => (
                        <div
                          key={delivery.id}
                          className="p-4 transition-colors hover:bg-gray-50"
                        >
                          <div className="flex items-start space-x-3">
                            <div
                              className={`p-2 rounded-lg ${
                                delivery.status === "DELIVERED"
                                  ? "bg-green-100"
                                  : delivery.status === "FAILED"
                                  ? "bg-red-100"
                                  : "bg-yellow-100"
                              }`}
                            >
                              {getStatusIcon(delivery.status)}
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium text-gray-900">
                                {delivery.eventType}
                              </p>
                              <p className="text-xs text-gray-600 truncate">
                                {delivery.webhookUrl}
                              </p>
                              <div className="flex items-center justify-between mt-1">
                                <span
                                  className={`text-xs px-2 py-0.5 rounded-full ${getStatusColor(
                                    delivery.status
                                  )}`}
                                >
                                  {delivery.status}
                                </span>
                                <span className="text-xs text-gray-500">
                                  {formatTimestamp(delivery.createdAt)}
                                </span>
                              </div>
                              {delivery.errorMessage && (
                                <p className="mt-1 text-xs text-red-600 truncate">
                                  {delivery.errorMessage}
                                </p>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                      {webhookDeliveries.length === 0 && (
                        <div className="p-8 text-center text-gray-500">
                          No deliveries yet
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
              <span className="flex items-center px-3 py-1 text-sm font-medium text-green-800 bg-green-100 rounded-full">
                <div className="w-2 h-2 mr-2 bg-green-500 rounded-full animate-pulse"></div>
                Live
              </span>
            </div>
          </div>
        </div>
      </header>

      {/* Error Toast */}
      {error && (
        <div className="fixed z-50 max-w-md px-4 py-3 text-red-700 bg-red-100 border border-red-400 rounded-lg shadow-lg top-20 right-4">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => setError(null)} className="ml-4">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Stats Cards */}
      <div className="px-4 py-6 mx-auto max-w-7xl sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-4 mb-6 md:grid-cols-4">
          <div className="p-5 bg-white border border-gray-200 rounded-lg shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">
                  Total Deliveries
                </p>
                <p className="mt-1 text-3xl font-bold text-gray-900">
                  {stats.totalDeliveries}
                </p>
              </div>
              <div className="p-3 bg-blue-100 rounded-lg">
                <Activity className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="p-5 bg-white border border-gray-200 rounded-lg shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Successful</p>
                <p className="mt-1 text-3xl font-bold text-green-600">
                  {stats.successfulDeliveries}
                </p>
              </div>
              <div className="p-3 bg-green-100 rounded-lg">
                <CheckCircle className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="p-5 bg-white border border-gray-200 rounded-lg shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Failed</p>
                <p className="mt-1 text-3xl font-bold text-red-600">
                  {stats.failedDeliveries}
                </p>
              </div>
              <div className="p-3 bg-red-100 rounded-lg">
                <XCircle className="w-6 h-6 text-red-600" />
              </div>
            </div>
          </div>

          <div className="p-5 bg-white border border-gray-200 rounded-lg shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">
                  Success Rate
                </p>
                <p className="mt-1 text-3xl font-bold text-blue-600">
                  {stats.successRate.toFixed(2)}%
                </p>
              </div>
              <div className="p-3 bg-purple-100 rounded-lg">
                <TrendingDown className="w-6 h-6 text-purple-600 rotate-180" />
              </div>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="mb-6 bg-white border border-gray-200 rounded-lg shadow-sm">
          <div className="border-b border-gray-200">
            <nav className="flex -mb-px">
              <button
                onClick={() => setActiveTab("products")}
                className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === "products"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                <div className="flex items-center space-x-2">
                  <Package className="w-4 h-4" />
                  <span>Products ({products.length})</span>
                </div>
              </button>
              <button
                onClick={() => setActiveTab("subscriptions")}
                className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === "subscriptions"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                <div className="flex items-center space-x-2">
                  <Webhook className="w-4 h-4" />
                  <span>Subscriptions ({webhookSubscriptions.length})</span>
                </div>
              </button>
              <button
                onClick={() => setActiveTab("deliveries")}
                className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === "deliveries"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                <div className="flex items-center space-x-2">
                  <Activity className="w-4 h-4" />
                  <span>Deliveries ({webhookDeliveries.length})</span>
                </div>
              </button>
            </nav>
          </div>

          {/* Products Tab */}
          {activeTab === "products" && (
            <div className="p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900">
                  Product Inventory
                </h2>
                <button
                  onClick={() => {
                    setProductForm(getInitialProductData());
                    setShowAddProduct(true);
                  }}
                  className="flex items-center px-4 py-2 space-x-2 text-white transition-colors bg-blue-600 rounded-lg hover:bg-blue-700"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Product</span>
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Product
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        SKU
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Price
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Stock
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Status
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {products.map((product) => (
                      <tr key={product.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <div className="flex items-center justify-center flex-shrink-0 w-10 h-10 bg-gray-200 rounded-lg">
                              <Package className="w-5 h-5 text-gray-500" />
                            </div>
                            <div className="ml-4">
                              <div className="text-sm font-medium text-gray-900">
                                {product.name}
                              </div>
                              <div className="text-sm text-gray-500">
                                {product.category || "N/A"}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="font-mono text-sm text-gray-900">
                            {product.sku}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-sm font-semibold text-gray-900">
                            ${product.price}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center space-x-2">
                            <span
                              className={`text-sm font-medium ${
                                product.stockQuantity <=
                                (product.lowStockThreshold || 10)
                                  ? "text-red-600"
                                  : "text-gray-900"
                              }`}
                            >
                              {product.stockQuantity} units
                            </span>
                            {product.stockQuantity <=
                              (product.lowStockThreshold || 10) && (
                              <AlertCircle className="w-4 h-4 text-red-500" />
                            )}
                          </div>
                          <div className="text-xs text-gray-500">
                            Threshold: {product.lowStockThreshold || 10}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(
                              product.status
                            )}`}
                          >
                            {product.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm whitespace-nowrap">
                          <div className="flex items-center space-x-2">
                            <button
                              onClick={() =>
                                handleStockUpdate(product.id, 5, "subtract")
                              }
                              className="px-3 py-1 text-red-700 transition-colors bg-red-100 rounded hover:bg-red-200"
                              title="Simulate order (-5 stock)"
                            >
                              <TrendingDown className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() =>
                                handleStockUpdate(product.id, 10, "add")
                              }
                              className="px-3 py-1 text-green-700 transition-colors bg-green-100 rounded hover:bg-green-200"
                              title="Add stock (+10 stock)"
                            >
                              <Archive className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() =>
                                handlePriceUpdate(product.id, product.price)
                              }
                              className="px-3 py-1 text-blue-700 transition-colors bg-blue-100 rounded hover:bg-blue-200"
                              title="Apply discount (-10%)"
                            >
                              <DollarSign className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => openEditProduct(product)}
                              className="px-3 py-1 text-gray-700 transition-colors bg-gray-100 rounded hover:bg-gray-200"
                              title="Edit product"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteProduct(product.id)}
                              className="px-3 py-1 text-red-700 transition-colors bg-red-100 rounded hover:bg-red-200"
                              title="Delete product"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {products.length === 0 && (
                  <div className="py-12 text-center text-gray-500">
                    No products found. Add your first product to get started.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Subscriptions Tab */}
          {activeTab === "subscriptions" && (
            <div className="p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900">
                  Webhook Subscriptions
                </h2>
                <button
                  onClick={() => setShowAddSubscription(true)}
                  className="flex items-center px-4 py-2 space-x-2 text-white transition-colors bg-blue-600 rounded-lg hover:bg-blue-700"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Subscription</span>
                </button>
              </div>

              <div className="space-y-4">
                {webhookSubscriptions.map((sub) => (
                  <div
                    key={sub.id}
                    className="p-4 transition-colors border border-gray-200 rounded-lg hover:border-blue-300"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center mb-2 space-x-3">
                          <h3 className="text-base font-semibold text-gray-900">
                            {sub.name}
                          </h3>
                          <span
                            className={`px-2 py-1 text-xs font-semibold rounded-full ${
                              sub.active
                                ? "bg-green-100 text-green-800"
                                : "bg-gray-100 text-gray-800"
                            }`}
                          >
                            {sub.active ? "Active" : "Inactive"}
                          </span>
                        </div>
                        <p className="mb-3 font-mono text-sm text-gray-600 break-all">
                          {sub.webhookUrl}
                        </p>
                        <div className="flex flex-wrap gap-2 mb-2">
                          {sub.subscribedEvents &&
                            sub.subscribedEvents.map((event, idx) => (
                              <span
                                key={idx}
                                className="px-2 py-1 text-xs font-medium text-blue-700 rounded-md bg-blue-50"
                              >
                                {event}
                              </span>
                            ))}
                        </div>
                        <p className="text-xs text-gray-500">
                          Last delivery: {getRelativeTime(sub.lastDeliveryAt)}
                        </p>
                      </div>
                      <div className="flex items-center ml-4 space-x-2">
                        <button
                          onClick={() => handleToggleSubscription(sub.id)}
                          className="p-2 text-gray-400 transition-colors hover:text-blue-600"
                          title="Toggle active"
                        >
                          <Settings className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleTestWebhook(sub.id)}
                          className="p-2 text-gray-400 transition-colors hover:text-green-600"
                          title="Test webhook"
                        >
                          <Activity className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteSubscription(sub.id)}
                          className="p-2 text-gray-400 transition-colors hover:text-red-600"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
                {webhookSubscriptions.length === 0 && (
                  <div className="py-12 text-center text-gray-500">
                    No webhook subscriptions found. Add a subscription to
                    receive webhook events.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Deliveries Tab */}
          {activeTab === "deliveries" && (
            <div className="p-6">
              <h2 className="mb-4 text-lg font-semibold text-gray-900">
                Recent Webhook Deliveries
              </h2>

              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Event
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Destination
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Status
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Attempts
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Time
                      </th>
                      <th className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {webhookDeliveries.map((delivery) => (
                      <tr key={delivery.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="font-mono text-sm text-gray-900">
                            {delivery.eventType}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className="text-sm text-gray-600 break-all">
                            {delivery.webhookUrl}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-3 py-1 inline-flex items-center space-x-1 text-xs leading-5 font-semibold rounded-full ${getStatusColor(
                              delivery.status
                            )}`}
                          >
                            {getStatusIcon(delivery.status)}
                            <span>{delivery.status}</span>
                          </span>
                          {delivery.errorMessage && (
                            <div
                              className="mt-1 text-xs text-red-600"
                              title={delivery.errorMessage}
                            >
                              {delivery.errorMessage.substring(0, 30)}...
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-sm text-gray-900">
                            {delivery.attempts}/{delivery.maxAttempts}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-sm text-gray-500">
                            {formatTimestamp(delivery.createdAt)}
                          </span>
                          {delivery.nextRetryAt && (
                            <div className="text-xs text-yellow-600">
                              Next: {formatTimestamp(delivery.nextRetryAt)}
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {delivery.status === "FAILED" ||
                          delivery.status === "RETRYING" ? (
                            <button
                              onClick={() => handleRetryDelivery(delivery.id)}
                              className="text-sm font-medium text-blue-600 hover:text-blue-800"
                            >
                              Retry
                            </button>
                          ) : (
                            <span className="text-sm text-gray-400">-</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {webhookDeliveries.length === 0 && (
                  <div className="py-12 text-center text-gray-500">
                    No webhook deliveries yet. Events will appear here when
                    products are modified.
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Live Activity Feed */}
        <div className="p-6 bg-white border border-gray-200 rounded-lg shadow-sm">
          <h2 className="flex items-center mb-4 text-lg font-semibold text-gray-900">
            <Activity className="w-5 h-5 mr-2 text-blue-600" />
            Live Activity Feed
          </h2>
          <div className="space-y-3 overflow-y-auto max-h-64">
            {webhookDeliveries.slice(0, 5).map((delivery) => (
              <div
                key={delivery.id}
                className="flex items-start p-3 space-x-3 rounded-lg bg-gray-50"
              >
                <div
                  className={`p-2 rounded-lg ${
                    delivery.status === "DELIVERED"
                      ? "bg-green-100"
                      : delivery.status === "FAILED"
                      ? "bg-red-100"
                      : "bg-yellow-100"
                  }`}
                >
                  {getStatusIcon(delivery.status)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900">
                    {delivery.eventType}
                  </p>
                  <p className="text-sm text-gray-600 truncate">
                    Sent to {delivery.webhookUrl}
                  </p>
                  <p className="mt-1 text-xs text-gray-500">
                    {formatTimestamp(delivery.createdAt)}
                  </p>
                </div>
              </div>
            ))}
            {webhookDeliveries.length === 0 && (
              <div className="py-8 text-center text-gray-500">
                No recent activity
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Add/Edit Product Modal */}
      {showAddProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 flex items-center justify-between p-6 bg-white border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">
                {editingProduct ? "Edit Product" : "Add New Product"}
              </h3>
              <button
                onClick={closeProductModal}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    SKU
                  </label>
                  <input
                    type="text"
                    value={productForm.sku}
                    onChange={(e) =>
                      setProductForm({ ...productForm, sku: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., LAPTOP-001"
                  />
                </div>
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Product Name
                  </label>
                  <input
                    type="text"
                    value={productForm.name}
                    onChange={(e) =>
                      setProductForm({ ...productForm, name: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., Gaming Laptop"
                  />
                </div>
              </div>

              <div>
                <label className="block mb-1 text-sm font-medium text-gray-700">
                  Description
                </label>
                <textarea
                  value={productForm.description}
                  onChange={(e) =>
                    setProductForm({
                      ...productForm,
                      description: e.target.value,
                    })
                  }
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Product description..."
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Price ($)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={productForm.price}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        price: parseFloat(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="99.99"
                  />
                </div>
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Stock Quantity
                  </label>
                  <input
                    type="number"
                    value={productForm.stockQuantity}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        stockQuantity: parseInt(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Low Stock Threshold
                  </label>
                  <input
                    type="number"
                    value={productForm.lowStockThreshold}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        lowStockThreshold: parseInt(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="10"
                  />
                </div>
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Status
                  </label>
                  <select
                    value={productForm.status}
                    onChange={(e) =>
                      setProductForm({ ...productForm, status: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="OUT_OF_STOCK">Out of Stock</option>
                    <option value="DISCONTINUED">Discontinued</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Category
                  </label>
                  <input
                    type="text"
                    value={productForm.category}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        category: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., Electronics"
                  />
                </div>
                <div>
                  <label className="block mb-1 text-sm font-medium text-gray-700">
                    Brand
                  </label>
                  <input
                    type="text"
                    value={productForm.brand}
                    onChange={(e) =>
                      setProductForm({ ...productForm, brand: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., TechBrand"
                  />
                </div>
              </div>

              <div>
                <label className="block mb-1 text-sm font-medium text-gray-700">
                  Image URL (optional)
                </label>
                <input
                  type="text"
                  value={productForm.imageUrl}
                  onChange={(e) =>
                    setProductForm({ ...productForm, imageUrl: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="https://example.com/image.jpg"
                />
              </div>
            </div>

            <div className="flex justify-end p-6 space-x-3 border-t border-gray-200 bg-gray-50">
              <button
                onClick={closeProductModal}
                className="px-4 py-2 text-gray-700 transition-colors border border-gray-300 rounded-lg hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={
                  editingProduct ? handleUpdateProduct : handleCreateProduct
                }
                disabled={loading}
                className="px-4 py-2 text-white transition-colors bg-blue-600 rounded-lg hover:bg-blue-700 disabled:bg-gray-400"
              >
                {loading
                  ? "Saving..."
                  : editingProduct
                  ? "Update Product"
                  : "Create Product"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Subscription Modal */}
      {showAddSubscription && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 flex items-center justify-between p-6 bg-white border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">
                Add Webhook Subscription
              </h3>
              <button
                onClick={() => {
                  setShowAddSubscription(false);
                  setSubscriptionForm(getInitialSubscriptionData());
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <label className="block mb-1 text-sm font-medium text-gray-700">
                  Subscription Name
                </label>
                <input
                  type="text"
                  value={subscriptionForm.name}
                  onChange={(e) =>
                    setSubscriptionForm({
                      ...subscriptionForm,
                      name: e.target.value,
                    })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="e.g., Warehouse System"
                />
              </div>

              <div>
                <label className="block mb-1 text-sm font-medium text-gray-700">
                  Webhook URL
                </label>
                <input
                  type="text"
                  value={subscriptionForm.webhookUrl}
                  onChange={(e) =>
                    setSubscriptionForm({
                      ...subscriptionForm,
                      webhookUrl: e.target.value,
                    })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="https://example.com/webhook"
                />
                <p className="mt-1 text-xs text-gray-500">
                  For testing, use: http://localhost:8080/test/webhook-receiver
                </p>
              </div>

              <div>
                <label className="block mb-2 text-sm font-medium text-gray-700">
                  Subscribed Events
                </label>
                <div className="space-y-2">
                  {availableEvents.map((event) => (
                    <label
                      key={event}
                      className="flex items-center space-x-2 cursor-pointer"
                    >
                      <input
                        type="checkbox"
                        checked={subscriptionForm.subscribedEvents.includes(
                          event
                        )}
                        onChange={() => handleEventToggle(event)}
                        className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                      />
                      <span className="text-sm text-gray-700">{event}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex justify-end p-6 space-x-3 border-t border-gray-200 bg-gray-50">
              <button
                onClick={() => {
                  setShowAddSubscription(false);
                  setSubscriptionForm(getInitialSubscriptionData());
                }}
                className="px-4 py-2 text-gray-700 transition-colors border border-gray-300 rounded-lg hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateSubscription}
                disabled={
                  loading || subscriptionForm.subscribedEvents.length === 0
                }
                className="px-4 py-2 text-white transition-colors bg-blue-600 rounded-lg hover:bg-blue-700 disabled:bg-gray-400"
              >
                {loading ? "Creating..." : "Create Subscription"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;