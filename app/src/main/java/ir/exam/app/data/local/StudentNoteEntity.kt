package ir.exam.app.data.local

import androidx.room.Entity

@Entity(tableName="student_notes",primaryKeys=["ownerUserId","studentId"])
data class StudentNoteEntity(val ownerUserId:String,val studentId:String,val note:String,val updatedAt:Long)
