import React, { FormEvent, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  BarChart3,
  Check,
  Cloud,
  CreditCard,
  Database,
  Download,
  Lock,
  LogOut,
  Plus,
  Receipt,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
  Smartphone,
  Trash2,
  Users,
} from "lucide-react";
import "./styles.css";

type Screen = "dashboard" | "customers" | "receipts" | "admin";
type CustomerStatus = "ACTIVE" | "COMPLETED" | "LAPSED";
type PaymentMethod = "UPI" | "CASH" | "CARD" | "BANK";

type Customer = {
  id: number;
  name: string;
  phone: string;
  email: string;
  monthlyAmount: number;
  amountPaid: number;
  startDate: number;
  maturityDate: number;
  status: CustomerStatus;
  initialSilverRate: number;
  notes: string;
};

type Payment = {
  id: number;
  customerId: number;
  installmentIndex: number;
  amount: number;
  paymentDate: number;
  paymentMethod: PaymentMethod;
  referenceNo: string;
  notes: string;
};

type AppData = {
  customers: Customer[];
  payments: Payment[];
  logs: string[];
};

const STORE_KEY = "sterling-vault-web-state-v1";
const MONTHS = 11;
const adminPin = "8888";

const seedCustomers: Customer[] = [
  makeCustomer(1, "Sarah Jenkins", "+91 98455 12091", "sarah.j@slvr.io", 5000, 88.5, "Scheme locked during Akshaya Tritiya"),
  makeCustomer(2, "Rajesh Kumar", "+91 97722 55431", "rajesh.kumar@gmail.com", 10000, 91.2, "Looking for heavy silver ornament sets upon maturity"),
  makeCustomer(3, "Elena Rostova", "+91 81232 44335", "elena.ros@icloud.com", 3500, 89, "Regular saver"),
];

const seedPayments: Payment[] = [
  makePayment(1, 1, 1, 5000, "UPI", "TXN7761"),
  makePayment(2, 1, 2, 5000, "UPI", "TXN9210"),
  makePayment(3, 1, 3, 5000, "UPI", "TXN1082"),
  makePayment(4, 2, 1, 10000, "UPI", "TXN4022"),
  makePayment(5, 2, 2, 10000, "BANK", "TXN0981"),
];

function makeCustomer(
  id: number,
  name: string,
  phone: string,
  email: string,
  monthlyAmount: number,
  initialSilverRate: number,
  notes: string,
): Customer {
  const startDate = Date.now() - id * 8 * 24 * 60 * 60 * 1000;
  return {
    id,
    name,
    phone,
    email,
    monthlyAmount,
    amountPaid: 0,
    startDate,
    maturityDate: startDate + MONTHS * 30 * 24 * 60 * 60 * 1000,
    status: "ACTIVE",
    initialSilverRate,
    notes,
  };
}

function makePayment(
  id: number,
  customerId: number,
  installmentIndex: number,
  amount: number,
  paymentMethod: PaymentMethod,
  referenceNo = "",
  notes = "",
): Payment {
  return {
    id,
    customerId,
    installmentIndex,
    amount,
    paymentDate: Date.now() - (MONTHS - installmentIndex) * 7 * 24 * 60 * 60 * 1000,
    paymentMethod,
    referenceNo,
    notes,
  };
}

function withPaidTotals(customers: Customer[], payments: Payment[]) {
  return customers.map((customer) => ({
    ...customer,
    amountPaid: payments
      .filter((payment) => payment.customerId === customer.id)
      .reduce((sum, payment) => sum + payment.amount, 0),
  }));
}

function loadData(): AppData {
  const raw = localStorage.getItem(STORE_KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as AppData;
      return { ...parsed, customers: withPaidTotals(parsed.customers, parsed.payments) };
    } catch {
      localStorage.removeItem(STORE_KEY);
    }
  }

  return {
    customers: withPaidTotals(seedCustomers, seedPayments),
    payments: seedPayments,
    logs: [formatLog("Seeded base ledger successfully. Cloud synchronized.")],
  };
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDate(value: number) {
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(value);
}

function formatLog(message: string) {
  return `[${new Intl.DateTimeFormat("en-IN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(Date.now())}] ${message}`;
}

function silverGrams(customer: Customer) {
  return customer.initialSilverRate > 0 ? customer.amountPaid / customer.initialSilverRate : 0;
}

function pendingBalance(customer: Customer) {
  return Math.max(customer.monthlyAmount * MONTHS - customer.amountPaid, 0);
}

function App() {
  const [screen, setScreen] = useState<Screen>("dashboard");
  const [data, setData] = useState<AppData>(() => loadData());
  const [search, setSearch] = useState("");
  const [offline, setOffline] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [pin, setPin] = useState("");
  const [adminUnlocked, setAdminUnlocked] = useState(false);
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(data.customers[0]?.id ?? null);

  useEffect(() => {
    localStorage.setItem(STORE_KEY, JSON.stringify(data));
  }, [data]);

  const customers = data.customers;
  const payments = data.payments;

  const selectedCustomer = customers.find((customer) => customer.id === selectedCustomerId) ?? customers[0];
  const filteredCustomers = customers.filter((customer) => {
    const text = `${customer.name} ${customer.phone} ${customer.email}`.toLowerCase();
    return text.includes(search.toLowerCase());
  });

  const metrics = useMemo(() => {
    const totalCollections = customers.reduce((sum, customer) => sum + customer.amountPaid, 0);
    const totalPending = customers.reduce((sum, customer) => sum + pendingBalance(customer), 0);
    const silverReserve = customers.reduce((sum, customer) => sum + silverGrams(customer), 0);
    return {
      activeCustomers: customers.filter((customer) => customer.status === "ACTIVE").length,
      totalCollections,
      totalPending,
      silverReserve,
    };
  }, [customers]);

  const monthlyCollections = useMemo(() => {
    const labels = Array.from({ length: 4 }, (_, index) => {
      const d = new Date();
      d.setMonth(d.getMonth() - (3 - index));
      return d.toLocaleString("en-IN", { month: "short" });
    });
    return labels.map((label) => ({
      label,
      amount: payments
        .filter((payment) => new Date(payment.paymentDate).toLocaleString("en-IN", { month: "short" }) === label)
        .reduce((sum, payment) => sum + payment.amount, 0),
    }));
  }, [payments]);

  function addLog(message: string) {
    setData((current) => ({ ...current, logs: [formatLog(message), ...current.logs].slice(0, 40) }));
  }

  function triggerSync() {
    if (offline) {
      addLog("Sync skipped: device is offline.");
      return;
    }
    setSyncing(true);
    addLog("Connecting to secure cloud cluster...");
    window.setTimeout(() => {
      addLog(`Uploaded ${customers.length} accounts and ${payments.length} receipts. Integrity verified.`);
      setSyncing(false);
    }, 900);
  }

  function saveCustomer(customer: Omit<Customer, "id" | "amountPaid" | "startDate" | "maturityDate" | "status">) {
    const id = Math.max(0, ...customers.map((item) => item.id)) + 1;
    const newCustomer: Customer = {
      ...customer,
      id,
      amountPaid: 0,
      startDate: Date.now(),
      maturityDate: Date.now() + MONTHS * 30 * 24 * 60 * 60 * 1000,
      status: "ACTIVE",
    };
    setData((current) => ({ ...current, customers: [...current.customers, newCustomer] }));
    setSelectedCustomerId(id);
    addLog(`Created local ledger for ${customer.name}.`);
  }

  function deleteCustomer(id: number) {
    const name = customers.find((item) => item.id === id)?.name ?? "customer";
    setData((current) => ({
      ...current,
      customers: current.customers.filter((customer) => customer.id !== id),
      payments: current.payments.filter((payment) => payment.customerId !== id),
    }));
    setSelectedCustomerId(customers.find((customer) => customer.id !== id)?.id ?? null);
    addLog(`Archived and deleted records for ${name}.`);
  }

  function addPayment(payment: Omit<Payment, "id" | "paymentDate">) {
    const id = Math.max(0, ...payments.map((item) => item.id)) + 1;
    const nextPayments = [...payments, { ...payment, id, paymentDate: Date.now() }];
    setData((current) => ({ ...current, payments: nextPayments, customers: withPaidTotals(current.customers, nextPayments) }));
    addLog(`Added receipt for installment #${payment.installmentIndex}.`);
  }

  function removePayment(id: number) {
    const nextPayments = payments.filter((payment) => payment.id !== id);
    setData((current) => ({ ...current, payments: nextPayments, customers: withPaidTotals(current.customers, nextPayments) }));
    addLog("Revoked receipt and recalculated ledger totals.");
  }

  function resetDemoData() {
    setData({
      customers: withPaidTotals(seedCustomers, seedPayments),
      payments: seedPayments,
      logs: [formatLog("Demo dataset restored.")],
    });
    setSelectedCustomerId(1);
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "sterling-vault-backup.json";
    anchor.click();
    URL.revokeObjectURL(url);
  }

  function verifyPin(value: string) {
    setPin(value);
    if (value.length === 4) {
      if (value === adminPin) {
        setAdminUnlocked(true);
        setPin("");
        addLog("Admin controls unlocked.");
      } else {
        setPin("");
        addLog("Admin unlock failed: invalid PIN.");
      }
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">SV</div>
          <div>
            <strong>Sterling Vault</strong>
            <span>Silver savings ledger</span>
          </div>
        </div>
        <nav aria-label="Primary">
          <NavButton icon={<BarChart3 />} label="Status" active={screen === "dashboard"} onClick={() => setScreen("dashboard")} />
          <NavButton icon={<Users />} label="Ledger" active={screen === "customers"} onClick={() => setScreen("customers")} />
          <NavButton icon={<Receipt />} label="Receipts" active={screen === "receipts"} onClick={() => setScreen("receipts")} />
          <NavButton icon={<Lock />} label="Admin" active={screen === "admin"} onClick={() => setScreen("admin")} />
        </nav>
        <div className="sync-card">
          <div className="sync-line">
            <Cloud size={18} />
            <strong>{offline ? "Offline mode" : syncing ? "Syncing" : "Cloud ready"}</strong>
          </div>
          <p>{payments.length} receipts mirrored across {customers.length} ledgers.</p>
        </div>
      </aside>

      <main>
        <header className="topbar">
          <div>
            <p className="eyebrow">Active portfolio</p>
            <h1>{screenTitle(screen)}</h1>
          </div>
          <div className="topbar-actions">
            <button className="icon-button" title="Export backup" onClick={exportJson}>
              <Download />
            </button>
            <button className="primary-button" onClick={triggerSync}>
              <RefreshCw size={18} />
              Sync
            </button>
          </div>
        </header>

        {screen === "dashboard" && (
          <Dashboard metrics={metrics} customers={customers} payments={payments} monthlyCollections={monthlyCollections} />
        )}

        {screen === "customers" && (
          <Customers
            customers={filteredCustomers}
            selectedCustomer={selectedCustomer}
            search={search}
            setSearch={setSearch}
            onSelect={setSelectedCustomerId}
            onSave={saveCustomer}
            onDelete={deleteCustomer}
          />
        )}

        {screen === "receipts" && (
          <Receipts customers={customers} payments={payments} selectedCustomerId={selectedCustomer?.id ?? null} onAdd={addPayment} onRemove={removePayment} />
        )}

        {screen === "admin" && (
          <Admin
            unlocked={adminUnlocked}
            pin={pin}
            offline={offline}
            syncing={syncing}
            logs={data.logs}
            onPin={verifyPin}
            onLock={() => setAdminUnlocked(false)}
            onToggleOffline={() => {
              setOffline((value) => !value);
              addLog(`Mode switched: ${offline ? "ONLINE Sync Connected" : "OFFLINE Mode Enabled"}.`);
            }}
            onSync={triggerSync}
            onReset={resetDemoData}
            onExport={exportJson}
          />
        )}
      </main>
    </div>
  );
}

function screenTitle(screen: Screen) {
  const titles: Record<Screen, string> = {
    dashboard: "Status command center",
    customers: "Customer ledger",
    receipts: "Receipt desk",
    admin: "Admin controls",
  };
  return titles[screen];
}

function NavButton({ icon, label, active, onClick }: { icon: React.ReactNode; label: string; active: boolean; onClick: () => void }) {
  return (
    <button className={active ? "nav-button active" : "nav-button"} onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

function Dashboard({
  metrics,
  customers,
  payments,
  monthlyCollections,
}: {
  metrics: { activeCustomers: number; totalCollections: number; totalPending: number; silverReserve: number };
  customers: Customer[];
  payments: Payment[];
  monthlyCollections: { label: string; amount: number }[];
}) {
  const maxMonth = Math.max(1, ...monthlyCollections.map((item) => item.amount));
  const newestPayments = [...payments].sort((a, b) => b.paymentDate - a.paymentDate).slice(0, 5);

  return (
    <section className="content-grid">
      <div className="metric-row">
        <Metric icon={<Users />} label="Active customers" value={metrics.activeCustomers.toString()} sub={`${customers.length} total ledgers`} />
        <Metric icon={<CreditCard />} label="Collections" value={formatCurrency(metrics.totalCollections)} sub={`${payments.length} receipts posted`} />
        <Metric icon={<Database />} label="Pending" value={formatCurrency(metrics.totalPending)} sub="Across 11 month plans" />
        <Metric icon={<ShieldCheck />} label="Silver reserve" value={`${metrics.silverReserve.toFixed(1)}g`} sub="At locked rates" />
      </div>

      <div className="panel wide">
        <div className="panel-title">
          <div>
            <p className="eyebrow">Collections</p>
            <h2>Four month movement</h2>
          </div>
          <span className="status-pill">Live</span>
        </div>
        <div className="bars">
          {monthlyCollections.map((item) => (
            <div className="bar-item" key={item.label}>
              <div className="bar-track">
                <div className="bar-fill" style={{ height: `${Math.max(8, (item.amount / maxMonth) * 100)}%` }} />
              </div>
              <strong>{item.label}</strong>
              <span>{formatCurrency(item.amount)}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="panel">
        <div className="panel-title">
          <div>
            <p className="eyebrow">Risk</p>
            <h2>Ledger health</h2>
          </div>
        </div>
        <div className="health-list">
          {customers.map((customer) => {
            const progress = Math.min(100, (customer.amountPaid / (customer.monthlyAmount * MONTHS)) * 100);
            return (
              <div className="health-row" key={customer.id}>
                <div>
                  <strong>{customer.name}</strong>
                  <span>{progress.toFixed(0)}% funded</span>
                </div>
                <progress value={progress} max={100} />
              </div>
            );
          })}
        </div>
      </div>

      <div className="panel">
        <div className="panel-title">
          <div>
            <p className="eyebrow">Recent</p>
            <h2>Receipts</h2>
          </div>
        </div>
        <div className="receipt-list">
          {newestPayments.map((payment) => {
            const customer = customers.find((item) => item.id === payment.customerId);
            return (
              <div className="receipt-row" key={payment.id}>
                <Receipt size={18} />
                <div>
                  <strong>{customer?.name ?? "Unknown"}</strong>
                  <span>{formatDate(payment.paymentDate)} via {payment.paymentMethod}</span>
                </div>
                <b>{formatCurrency(payment.amount)}</b>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function Metric({ icon, label, value, sub }: { icon: React.ReactNode; label: string; value: string; sub: string }) {
  return (
    <div className="metric">
      <div className="metric-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{sub}</small>
    </div>
  );
}

function Customers({
  customers,
  selectedCustomer,
  search,
  setSearch,
  onSelect,
  onSave,
  onDelete,
}: {
  customers: Customer[];
  selectedCustomer?: Customer;
  search: string;
  setSearch: (value: string) => void;
  onSelect: (id: number) => void;
  onSave: (customer: Omit<Customer, "id" | "amountPaid" | "startDate" | "maturityDate" | "status">) => void;
  onDelete: (id: number) => void;
}) {
  return (
    <section className="split-layout">
      <div className="panel list-panel">
        <div className="search-box">
          <Search size={18} />
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search customer, mobile, email..." />
        </div>
        <div className="customer-list">
          {customers.map((customer) => (
            <button className={selectedCustomer?.id === customer.id ? "customer-row selected" : "customer-row"} key={customer.id} onClick={() => onSelect(customer.id)}>
              <span>{customer.name}</span>
              <small>{customer.phone}</small>
              <b>{formatCurrency(customer.amountPaid)}</b>
            </button>
          ))}
        </div>
      </div>

      <div className="panel detail-panel">
        {selectedCustomer ? (
          <>
            <div className="panel-title">
              <div>
                <p className="eyebrow">Customer</p>
                <h2>{selectedCustomer.name}</h2>
              </div>
              <button className="danger-button" onClick={() => onDelete(selectedCustomer.id)} title="Delete customer">
                <Trash2 size={17} />
              </button>
            </div>
            <div className="detail-stats">
              <Metric icon={<CreditCard />} label="Paid" value={formatCurrency(selectedCustomer.amountPaid)} sub={`${silverGrams(selectedCustomer).toFixed(1)}g silver`} />
              <Metric icon={<Database />} label="Pending" value={formatCurrency(pendingBalance(selectedCustomer))} sub={`${MONTHS} month plan`} />
            </div>
            <dl className="info-grid">
              <div><dt>Phone</dt><dd>{selectedCustomer.phone}</dd></div>
              <div><dt>Email</dt><dd>{selectedCustomer.email || "Not set"}</dd></div>
              <div><dt>Monthly</dt><dd>{formatCurrency(selectedCustomer.monthlyAmount)}</dd></div>
              <div><dt>Locked rate</dt><dd>INR {selectedCustomer.initialSilverRate}/g</dd></div>
              <div><dt>Maturity</dt><dd>{formatDate(selectedCustomer.maturityDate)}</dd></div>
              <div><dt>Notes</dt><dd>{selectedCustomer.notes || "None"}</dd></div>
            </dl>
          </>
        ) : (
          <EmptyState title="No customer selected" />
        )}
      </div>

      <CustomerForm onSave={onSave} />
    </section>
  );
}

function CustomerForm({ onSave }: { onSave: (customer: Omit<Customer, "id" | "amountPaid" | "startDate" | "maturityDate" | "status">) => void }) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [monthlyAmount, setMonthlyAmount] = useState("5000");
  const [initialSilverRate, setInitialSilverRate] = useState("90");
  const [notes, setNotes] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!name.trim() || !phone.trim()) return;
    onSave({
      name: name.trim(),
      phone: phone.trim(),
      email: email.trim(),
      monthlyAmount: Number(monthlyAmount) || 0,
      initialSilverRate: Number(initialSilverRate) || 0,
      notes: notes.trim(),
    });
    setName("");
    setPhone("");
    setEmail("");
    setMonthlyAmount("5000");
    setInitialSilverRate("90");
    setNotes("");
  }

  return (
    <form className="panel form-panel" onSubmit={submit}>
      <div className="panel-title">
        <div>
          <p className="eyebrow">New</p>
          <h2>Add customer</h2>
        </div>
        <Plus size={20} />
      </div>
      <Field label="Name" value={name} onChange={setName} required />
      <Field label="Phone" value={phone} onChange={setPhone} required />
      <Field label="Email" value={email} onChange={setEmail} />
      <div className="form-grid">
        <Field label="Monthly INR" value={monthlyAmount} onChange={setMonthlyAmount} type="number" required />
        <Field label="Rate INR/g" value={initialSilverRate} onChange={setInitialSilverRate} type="number" required />
      </div>
      <Field label="Notes" value={notes} onChange={setNotes} />
      <button className="primary-button" type="submit">
        <Save size={18} />
        Save customer
      </button>
    </form>
  );
}

function Receipts({
  customers,
  payments,
  selectedCustomerId,
  onAdd,
  onRemove,
}: {
  customers: Customer[];
  payments: Payment[];
  selectedCustomerId: number | null;
  onAdd: (payment: Omit<Payment, "id" | "paymentDate">) => void;
  onRemove: (id: number) => void;
}) {
  const [customerId, setCustomerId] = useState(selectedCustomerId?.toString() ?? "");
  const [installmentIndex, setInstallmentIndex] = useState("1");
  const [amount, setAmount] = useState("5000");
  const [method, setMethod] = useState<PaymentMethod>("UPI");
  const [referenceNo, setReferenceNo] = useState("");
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (selectedCustomerId) setCustomerId(selectedCustomerId.toString());
  }, [selectedCustomerId]);

  function submit(event: FormEvent) {
    event.preventDefault();
    const parsedCustomerId = Number(customerId);
    if (!parsedCustomerId) return;
    onAdd({
      customerId: parsedCustomerId,
      installmentIndex: Number(installmentIndex) || 1,
      amount: Number(amount) || 0,
      paymentMethod: method,
      referenceNo,
      notes,
    });
    setReferenceNo("");
    setNotes("");
  }

  return (
    <section className="receipt-layout">
      <form className="panel form-panel" onSubmit={submit}>
        <div className="panel-title">
          <div>
            <p className="eyebrow">Receipt</p>
            <h2>Record payment</h2>
          </div>
          <Smartphone size={20} />
        </div>
        <label className="field">
          <span>Customer</span>
          <select value={customerId} onChange={(event) => setCustomerId(event.target.value)} required>
            <option value="">Select customer</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>{customer.name}</option>
            ))}
          </select>
        </label>
        <div className="form-grid">
          <Field label="Installment" value={installmentIndex} onChange={setInstallmentIndex} type="number" required />
          <Field label="Amount" value={amount} onChange={setAmount} type="number" required />
        </div>
        <label className="field">
          <span>Method</span>
          <select value={method} onChange={(event) => setMethod(event.target.value as PaymentMethod)}>
            <option>UPI</option>
            <option>CASH</option>
            <option>CARD</option>
            <option>BANK</option>
          </select>
        </label>
        <Field label="Reference" value={referenceNo} onChange={setReferenceNo} />
        <Field label="Notes" value={notes} onChange={setNotes} />
        <button className="primary-button" type="submit">
          <Check size={18} />
          Post receipt
        </button>
      </form>

      <div className="panel wide">
        <div className="panel-title">
          <div>
            <p className="eyebrow">Ledger</p>
            <h2>Receipt history</h2>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Customer</th>
                <th>Date</th>
                <th>Method</th>
                <th>Reference</th>
                <th>Amount</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {[...payments].sort((a, b) => b.paymentDate - a.paymentDate).map((payment) => {
                const customer = customers.find((item) => item.id === payment.customerId);
                return (
                  <tr key={payment.id}>
                    <td>{customer?.name ?? "Unknown"}</td>
                    <td>{formatDate(payment.paymentDate)}</td>
                    <td>{payment.paymentMethod}</td>
                    <td>{payment.referenceNo || "-"}</td>
                    <td>{formatCurrency(payment.amount)}</td>
                    <td>
                      <button className="icon-button small" title="Delete receipt" onClick={() => onRemove(payment.id)}>
                        <Trash2 />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

function Admin({
  unlocked,
  pin,
  offline,
  syncing,
  logs,
  onPin,
  onLock,
  onToggleOffline,
  onSync,
  onReset,
  onExport,
}: {
  unlocked: boolean;
  pin: string;
  offline: boolean;
  syncing: boolean;
  logs: string[];
  onPin: (value: string) => void;
  onLock: () => void;
  onToggleOffline: () => void;
  onSync: () => void;
  onReset: () => void;
  onExport: () => void;
}) {
  if (!unlocked) {
    return (
      <section className="admin-lock">
        <div className="panel pin-panel">
          <ShieldCheck size={36} />
          <h2>Secure admin access</h2>
          <div className="pin-dots">{Array.from({ length: 4 }, (_, index) => <span className={pin.length > index ? "filled" : ""} key={index} />)}</div>
          <div className="pin-grid">
            {"123456789".split("").map((digit) => (
              <button key={digit} onClick={() => onPin(pin + digit)}>{digit}</button>
            ))}
            <button onClick={() => onPin("")}>Clear</button>
            <button onClick={() => onPin(pin + "0")}>0</button>
            <button onClick={() => onPin(pin.slice(0, -1))}>Back</button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="content-grid">
      <div className="metric-row">
        <button className={offline ? "metric action active" : "metric action"} onClick={onToggleOffline}>
          <Cloud />
          <span>Network mode</span>
          <strong>{offline ? "Offline" : "Online"}</strong>
          <small>Toggle local-first behavior</small>
        </button>
        <button className="metric action" onClick={onSync}>
          <RefreshCw />
          <span>Cloud mirror</span>
          <strong>{syncing ? "Syncing" : "Ready"}</strong>
          <small>Push local ledger state</small>
        </button>
        <button className="metric action" onClick={onExport}>
          <Download />
          <span>Backup</span>
          <strong>JSON</strong>
          <small>Download ledger snapshot</small>
        </button>
        <button className="metric action danger" onClick={onReset}>
          <Database />
          <span>Demo data</span>
          <strong>Reset</strong>
          <small>Restore sample portfolio</small>
        </button>
      </div>
      <div className="panel wide">
        <div className="panel-title">
          <div>
            <p className="eyebrow">Operations</p>
            <h2>Sync log</h2>
          </div>
          <button className="secondary-button" onClick={onLock}>
            <LogOut size={18} />
            Lock
          </button>
        </div>
        <div className="log-list">
          {logs.map((log, index) => <code key={`${log}-${index}`}>{log}</code>)}
        </div>
      </div>
    </section>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  required = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input value={value} type={type} required={required} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function EmptyState({ title }: { title: string }) {
  return (
    <div className="empty-state">
      <Database size={26} />
      <strong>{title}</strong>
    </div>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
