import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import {
  Calendar, Plus, Check, X, CheckCheck, Filter, Search,
} from 'lucide-react'
import { AppointmentAPI } from '../../api/client.js'
import {
  LoadingBlock, Modal, SectionHeader, StatusBadge, EmptyState, formatDateTime,
} from '../../components/ui.jsx'
import { useAuth } from '../../context/AuthContext.jsx'

export default function DoctorAppointments() {
  const { getLinkedDoctorId } = useAuth()
  const defaultDoctorId = getLinkedDoctorId() || ''
  const [loading, setLoading] = useState(false)
  const [appts, setAppts] = useState([])
  const [filters, setFilters] = useState({
    doctorId: defaultDoctorId,
    patientId: '',
    status: 'ALL',
  })
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const params = {}
      if (filters.doctorId)  params.doctorId  = filters.doctorId
      if (filters.patientId) params.patientId = filters.patientId
      if (filters.status !== 'ALL') params.status = filters.status
      const { data } = await AppointmentAPI.search(params)
      setAppts(data || [])
    } catch (err) {
      toast.error('Could not fetch appointments')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // initial

  const filtered = !search ? appts : appts.filter((a) =>
    [a.id, a.patientId, a.doctorId, a.reason]
      .filter(Boolean).some((v) => String(v).toLowerCase().includes(search.toLowerCase())))

  async function confirm(id) {
    try { await AppointmentAPI.confirm(id);  toast.success('Confirmed');  load() }
    catch (err) { toast.error(err.response?.data?.error || 'Could not confirm') }
  }
  async function cancel(id) {
    try { await AppointmentAPI.cancel(id);   toast.success('Cancelled');  load() }
    catch (err) { toast.error(err.response?.data?.error || 'Could not cancel') }
  }
  async function complete(id) {
    try { await AppointmentAPI.complete(id); toast.success('Marked complete'); load() }
    catch (err) { toast.error(err.response?.data?.error || 'Could not complete') }
  }

  return (
    <div>
      <SectionHeader
        kicker="Clinical"
        title="Appointments"
        subtitle="Create, confirm, cancel and complete visits — all in one queue."
        action={
          <button className="btn-primary" onClick={() => setShowCreate(true)}>
            <Plus size={16} /> New appointment
          </button>
        }
      />

      {/* Filters */}
      <div className="card card-pad mb-6">
        <form onSubmit={(e) => { e.preventDefault(); load() }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 items-end">
          <div>
            <label className="label">Doctor ID</label>
            <input className="input" placeholder="4"
              value={filters.doctorId}
              onChange={(e) => setFilters({ ...filters, doctorId: e.target.value })} />
          </div>
          <div>
            <label className="label">Patient ID</label>
            <input className="input" placeholder="12"
              value={filters.patientId}
              onChange={(e) => setFilters({ ...filters, patientId: e.target.value })} />
          </div>
          <div>
            <label className="label">Status</label>
            <select className="select"
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
              {['ALL','PENDING','CONFIRMED','CANCELLED','COMPLETED'].map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-1">
            <label className="label">Quick search</label>
            <div className="relative">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
              <input className="input pl-9" placeholder="ID or reason"
                value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>
          </div>
          <button className="btn-secondary">
            <Filter size={14} /> Apply
          </button>
        </form>
      </div>

      {loading ? (
        <LoadingBlock />
      ) : filtered.length === 0 ? (
        <div className="card">
          <EmptyState icon={Calendar} title="No appointments" body="Try changing filters or create a new one." />
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="hms-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Patient</th>
                  <th>Doctor</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => (
                  <tr key={a.id}>
                    <td className="font-mono text-xs text-ink-500">#{a.id}</td>
                    <td className="tabular-nums">#{a.patientId}</td>
                    <td className="tabular-nums">#{a.doctorId}</td>
                    <td className="whitespace-nowrap">{formatDateTime(a.startTime)}</td>
                    <td className="whitespace-nowrap text-ink-500">{formatDateTime(a.endTime)}</td>
                    <td className="max-w-[200px] truncate">{a.reason || '—'}</td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>
                      <div className="flex items-center gap-1 justify-end">
                        {a.status === 'PENDING' && (
                          <button className="btn-sm bg-sky-50 text-sky-700 border border-sky-100 hover:bg-sky-100 inline-flex items-center gap-1 rounded-lg"
                            onClick={() => confirm(a.id)}>
                            <Check size={13} /> Confirm
                          </button>
                        )}
                        {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                          <button className="btn-sm bg-rose-50 text-rose-700 border border-rose-100 hover:bg-rose-100 inline-flex items-center gap-1 rounded-lg"
                            onClick={() => cancel(a.id)}>
                            <X size={13} /> Cancel
                          </button>
                        )}
                        {a.status === 'CONFIRMED' && (
                          <button className="btn-sm bg-brand-600 text-white hover:bg-brand-700 inline-flex items-center gap-1 rounded-lg"
                            onClick={() => complete(a.id)}>
                            <CheckCheck size={13} /> Complete
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <CreateAppointmentModal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        defaultDoctorId={filters.doctorId}
        onCreated={() => { setShowCreate(false); load() }}
      />
    </div>
  )
}

function CreateAppointmentModal({ open, onClose, defaultDoctorId, onCreated }) {
  const [form, setForm] = useState({
    patientId: '', doctorId: defaultDoctorId || '', startTime: '', reason: '',
  })
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (open) setForm({ patientId: '', doctorId: defaultDoctorId || '', startTime: '', reason: '' })
  }, [open, defaultDoctorId])

  async function submit(e) {
    e.preventDefault()
    if (!form.patientId || !form.doctorId || !form.startTime) {
      toast.error('Patient, doctor and start time are required'); return
    }
    setBusy(true)
    try {
      // Backend expects OffsetDateTime → append seconds + Z (UTC)
      const iso = new Date(form.startTime).toISOString()
      await AppointmentAPI.create({
        patientId: Number(form.patientId),
        doctorId:  Number(form.doctorId),
        startTime: iso,
        reason: form.reason,
      })
      toast.success('Appointment created')
      onCreated?.()
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || 'Create failed'
      toast.error(msg)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New appointment">
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Patient ID</label>
            <input className="input" value={form.patientId}
              onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
          </div>
          <div>
            <label className="label">Doctor ID</label>
            <input className="input" value={form.doctorId}
              onChange={(e) => setForm({ ...form, doctorId: e.target.value })} />
          </div>
        </div>
        <div>
          <label className="label">Start time</label>
          <input type="datetime-local" className="input" value={form.startTime}
            onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
          <p className="text-[11px] text-ink-500 mt-1">
            The backend validates against the doctor’s weekly schedule and blocks overlap.
          </p>
        </div>
        <div>
          <label className="label">Reason</label>
          <textarea className="textarea" value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })}
            placeholder="Follow-up for hypertension…" />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={busy}>
            {busy ? 'Creating…' : 'Create appointment'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
