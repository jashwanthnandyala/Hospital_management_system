import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Plus, Edit2, Trash2, Clock, ToggleLeft, ToggleRight } from 'lucide-react'
import { ScheduleAPI } from '../../api/client.js'
import {
  LoadingBlock, Modal, SectionHeader, EmptyState,
} from '../../components/ui.jsx'
import { useAuth } from '../../context/AuthContext.jsx'

const DAYS = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']
const DAY_SHORT = { MONDAY:'Mon', TUESDAY:'Tue', WEDNESDAY:'Wed', THURSDAY:'Thu', FRIDAY:'Fri', SATURDAY:'Sat', SUNDAY:'Sun' }

const empty = {
  doctorId: '', dayOfWeek: 'MONDAY',
  startTime: '09:00', endTime: '17:00',
  slotDurationMinutes: 30, active: true,
}

export default function DoctorSchedules() {
  const { getLinkedDoctorId } = useAuth()
  const [doctorId, setDoctorId] = useState(getLinkedDoctorId() || '')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [onlyActive, setOnlyActive] = useState(false)
  const [editing, setEditing] = useState(null) // null | 'new' | schedule obj
  const [confirmDelete, setConfirmDelete] = useState(null)

  async function load() {
    if (!doctorId) { setItems([]); return }
    setLoading(true)
    try {
      const { data } = onlyActive
        ? await ScheduleAPI.listActive(doctorId)
        : await ScheduleAPI.list(doctorId)
      setItems(data || [])
    } catch { toast.error('Could not load schedules') }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [doctorId, onlyActive])

  async function remove() {
    if (!confirmDelete) return
    try {
      await ScheduleAPI.delete(confirmDelete.id)
      toast.success('Schedule deleted')
      setConfirmDelete(null); load()
    } catch { toast.error('Delete failed') }
  }

  return (
    <div>
      <SectionHeader
        kicker="Availability"
        title="Weekly schedule"
        subtitle="Publish when you see patients. Appointments can only be booked inside these windows."
        action={
          <button className="btn-primary" onClick={() => setEditing('new')} disabled={!doctorId}>
            <Plus size={16} /> Add schedule
          </button>
        }
      />

      <div className="card card-pad mb-6 flex flex-col sm:flex-row sm:items-end gap-3">
        <div className="sm:w-48">
          <label className="label">Doctor ID</label>
          <input className="input" value={doctorId}
            onChange={(e) => setDoctorId(e.target.value)}
            placeholder="e.g. 4" />
        </div>
        <button
          type="button"
          onClick={() => setOnlyActive(!onlyActive)}
          className={`btn-secondary ${onlyActive ? '!border-brand-300 !bg-brand-50 !text-brand-800' : ''}`}>
          {onlyActive ? <ToggleRight size={16} /> : <ToggleLeft size={16} />}
          Only active
        </button>
      </div>

      {loading ? <LoadingBlock /> : !doctorId ? (
        <div className="card"><EmptyState icon={Clock} title="Enter your doctor ID" body="Schedules are per-doctor." /></div>
      ) : items.length === 0 ? (
        <div className="card"><EmptyState icon={Clock} title="No schedules yet" body="Create the first slot to start accepting appointments." /></div>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {items.map((s) => (
            <div key={s.id} className="card card-pad">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="text-xs uppercase tracking-wider font-semibold text-brand-700">
                    {DAY_SHORT[s.dayOfWeek] || s.dayOfWeek}
                  </div>
                  <div className="text-2xl font-display text-ink-900">
                    {trimTime(s.startTime)} – {trimTime(s.endTime)}
                  </div>
                </div>
                <span className={`badge ${s.active ? 'bg-brand-100 text-brand-800' : 'bg-ink-200 text-ink-600'}`}>
                  {s.active ? 'Active' : 'Paused'}
                </span>
              </div>
              <div className="text-sm text-ink-500">Slots of {s.slotDurationMinutes || 30} min</div>
              <div className="mt-4 flex gap-2">
                <button className="btn-secondary btn-sm flex-1" onClick={() => setEditing(s)}>
                  <Edit2 size={13} /> Edit
                </button>
                <button className="btn-ghost btn-sm hover:!text-rose-600"
                  onClick={() => setConfirmDelete(s)}>
                  <Trash2 size={13} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ScheduleForm
        open={!!editing}
        editing={editing}
        doctorId={doctorId}
        onClose={() => setEditing(null)}
        onSaved={() => { setEditing(null); load() }}
      />

      <Modal open={!!confirmDelete} onClose={() => setConfirmDelete(null)}
             title="Delete schedule?" maxWidth="max-w-md">
        <p className="text-sm text-ink-600 mb-5">
          {confirmDelete && `This removes the ${confirmDelete.dayOfWeek.toLowerCase()} window ${trimTime(confirmDelete.startTime)}–${trimTime(confirmDelete.endTime)}.`}
        </p>
        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={() => setConfirmDelete(null)}>Cancel</button>
          <button className="btn-danger" onClick={remove}>Delete</button>
        </div>
      </Modal>
    </div>
  )
}

function trimTime(t) {
  if (!t) return '—'
  const str = String(t)
  return str.length >= 5 ? str.slice(0, 5) : str
}

function ScheduleForm({ open, editing, doctorId, onClose, onSaved }) {
  const isEdit = editing && editing !== 'new'
  const [form, setForm] = useState(empty)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open) return
    if (isEdit) {
      setForm({
        doctorId: doctorId,
        dayOfWeek: editing.dayOfWeek,
        startTime: trimTime(editing.startTime),
        endTime: trimTime(editing.endTime),
        slotDurationMinutes: editing.slotDurationMinutes || 30,
        active: !!editing.active,
      })
    } else {
      setForm({ ...empty, doctorId: doctorId || '' })
    }
  }, [open, editing, doctorId, isEdit])

  async function submit(e) {
    e.preventDefault()
    if (!form.doctorId) { toast.error('Doctor ID required'); return }
    const payload = {
      doctorId: Number(form.doctorId),
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime.length === 5 ? `${form.startTime}:00` : form.startTime,
      endTime: form.endTime.length === 5 ? `${form.endTime}:00` : form.endTime,
      slotDurationMinutes: Number(form.slotDurationMinutes) || 30,
      active: !!form.active,
    }
    setBusy(true)
    try {
      if (isEdit) {
        await ScheduleAPI.update(editing.id, payload)
        toast.success('Schedule updated')
      } else {
        await ScheduleAPI.create(payload)
        toast.success('Schedule created')
      }
      onSaved?.()
    } catch (err) {
      toast.error(err.response?.data?.error || 'Save failed')
    } finally { setBusy(false) }
  }

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Edit schedule' : 'Add schedule'}>
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Doctor ID</label>
            <input className="input" value={form.doctorId}
              onChange={(e) => setForm({ ...form, doctorId: e.target.value })} />
          </div>
          <div>
            <label className="label">Day of week</label>
            <select className="select" value={form.dayOfWeek}
              onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}>
              {DAYS.map((d) => <option key={d}>{d}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Start time</label>
            <input type="time" className="input" value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
          </div>
          <div>
            <label className="label">End time</label>
            <input type="time" className="input" value={form.endTime}
              onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          </div>
          <div>
            <label className="label">Slot duration (min)</label>
            <input type="number" className="input" value={form.slotDurationMinutes}
              onChange={(e) => setForm({ ...form, slotDurationMinutes: e.target.value })} />
          </div>
          <div>
            <label className="label">Active</label>
            <button type="button"
              onClick={() => setForm({ ...form, active: !form.active })}
              className={`input text-left flex items-center gap-2 ${form.active ? 'text-brand-700' : 'text-ink-500'}`}>
              {form.active ? <ToggleRight size={18} /> : <ToggleLeft size={18} />}
              {form.active ? 'Yes, accepting' : 'Paused'}
            </button>
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={busy}>
            {busy ? 'Saving…' : isEdit ? 'Save changes' : 'Create schedule'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
