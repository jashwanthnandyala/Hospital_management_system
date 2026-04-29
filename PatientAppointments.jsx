import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Link } from 'react-router-dom'
import { Calendar, ArrowRight, Ban } from 'lucide-react'
import { AppointmentAPI } from '../../api/client.js'
import { useAuth } from '../../context/AuthContext.jsx'
import {
  LoadingBlock, Modal, SectionHeader, StatusBadge, EmptyState, formatDateTime,
} from '../../components/ui.jsx'
import { NoPatient } from './PatientAddresses.jsx'

export default function PatientAppointments() {
  const { getLinkedPatientId } = useAuth()
  const patientId = getLinkedPatientId()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [confirmCancel, setConfirmCancel] = useState(null)

  async function load() {
    if (!patientId) return
    setLoading(true)
    try {
      const params = { patientId }
      if (statusFilter !== 'ALL') params.status = statusFilter
      const { data } = await AppointmentAPI.search(params)
      setItems(data || [])
    } catch { toast.error('Could not fetch appointments') }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [patientId, statusFilter])

  async function cancel() {
    if (!confirmCancel) return
    try {
      await AppointmentAPI.cancel(confirmCancel.id)
      toast.success('Appointment cancelled')
      setConfirmCancel(null); load()
    } catch (err) {
      toast.error(err.response?.data?.error || 'Cancel failed')
    }
  }

  if (!patientId) return <NoPatient />

  return (
    <div>
      <SectionHeader
        kicker="My care"
        title="My appointments"
        subtitle="All visits booked in your name — past, present and planned."
        action={
          <Link to="/patient/book" className="btn-primary">
            Book new <ArrowRight size={16} />
          </Link>
        }
      />

      <div className="flex flex-wrap items-center gap-2 mb-6">
        {['ALL','PENDING','CONFIRMED','COMPLETED','CANCELLED'].map((s) => (
          <button key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 text-xs font-semibold uppercase tracking-wider rounded-lg ${
              statusFilter === s
                ? 'bg-ink-900 text-white'
                : 'bg-ink-100 text-ink-700 hover:bg-ink-200'
            }`}>
            {s}
          </button>
        ))}
      </div>

      {loading ? <LoadingBlock /> : items.length === 0 ? (
        <div className="card">
          <EmptyState icon={Calendar}
            title="No appointments"
            body={statusFilter === 'ALL' ? 'Book your first visit.' : `No ${statusFilter.toLowerCase()} appointments.`}
            action={<Link to="/patient/book" className="btn-primary">Book a visit</Link>} />
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((a) => (
            <div key={a.id} className="card card-pad flex flex-col sm:flex-row sm:items-center gap-4">
              <div className="w-14 h-14 rounded-xl bg-brand-50 border border-brand-100 flex flex-col items-center justify-center leading-none tabular-nums shrink-0">
                <span className="text-[10px] text-brand-700 uppercase">
                  {new Date(a.startTime).toLocaleDateString(undefined, { month: 'short' })}
                </span>
                <span className="text-xl font-semibold text-brand-900">
                  {new Date(a.startTime).getDate()}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-semibold text-ink-900">Dr. #{a.doctorId}</span>
                  <StatusBadge status={a.status} />
                </div>
                <div className="text-sm text-ink-600 mt-0.5">
                  {formatDateTime(a.startTime)}
                  {a.endTime && <span className="text-ink-400"> – {formatDateTime(a.endTime)}</span>}
                </div>
                {a.reason && <div className="text-sm text-ink-500 mt-1">Reason: {a.reason}</div>}
              </div>
              {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                <button className="btn-secondary btn-sm hover:!text-rose-600"
                  onClick={() => setConfirmCancel(a)}>
                  <Ban size={13} /> Cancel
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      <Modal open={!!confirmCancel} onClose={() => setConfirmCancel(null)}
             title="Cancel this appointment?" maxWidth="max-w-md">
        <p className="text-sm text-ink-600 mb-5">
          Your slot with Dr. #{confirmCancel?.doctorId} on {formatDateTime(confirmCancel?.startTime)} will be freed.
        </p>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={() => setConfirmCancel(null)}>Keep it</button>
          <button className="btn-danger" onClick={cancel}>Cancel appointment</button>
        </div>
      </Modal>
    </div>
  )
}
