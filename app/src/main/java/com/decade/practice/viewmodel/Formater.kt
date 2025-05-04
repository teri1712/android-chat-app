package com.decade.practice.viewmodel

import com.decade.practice.model.presentation.Message
import com.decade.practice.model.presentation.Position
import javax.inject.Inject

interface Formater {
      fun reformat(messageList: List<Message>)
      fun format(messageList: List<Message>)
}

class RollInFormater @Inject constructor() : Formater {

      override fun reformat(messageList: List<Message>) {
            val twoMin = 2 * 60 * 1000
            if (messageList.isEmpty())
                  return
            val size = messageList.size
            val first = messageList.first()

            first.position = Position.Single
            if (size == 1) {
                  return
            }

            for (i in 0 until size - 1) {
                  val cur = messageList[i]
                  val above = messageList[i + 1]
                  cur.fixedDisplayTime = cur.receiveTime - above.receiveTime >= twoMin && cur.sender == above.sender
                  if (cur.iconEvent != null)
                        break
                  if (cur.receiveTime - above.receiveTime >= twoMin
                        || cur.sender != above.sender
                        || above.iconEvent != null
                  ) {
                        break
                  }
                  if (cur.position == Position.Single) {
                        cur.position = Position.Bottom
                  } else {
                        cur.position = Position.Center
                  }
                  above.position = Position.Top
            }

            val last = messageList.last()
            last.position = Position.Single
            last.fixedDisplayTime = true

            for (i in size - 1 downTo 1) {
                  val cur = messageList[i]
                  val below = messageList[i - 1]
                  below.fixedDisplayTime = below.receiveTime - cur.receiveTime >= twoMin && cur.sender == below.sender
                  if (cur.iconEvent != null)
                        break
                  if (below.receiveTime - cur.receiveTime >= twoMin
                        || cur.sender != below.sender
                        || below.iconEvent != null
                  ) {
                        break
                  }
                  if (cur.position == Position.Single) {
                        cur.position = Position.Top
                  } else {
                        cur.position = Position.Center
                  }
                  below.position = Position.Bottom
            }
      }

      override fun format(messageList: List<Message>) {
            for (i in 1 until messageList.size + 1) {
                  reformat(messageList.subList(0, i))
            }
      }

}