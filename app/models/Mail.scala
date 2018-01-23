package models

/**
  * Represents an e-mail.
  *
  * @author Johann Sell
  * @param user references the user that has draft the e-mail and initiated the sending.
  * @param subject of the e-mail.
  * @param body of the e-mail (Text or HTML).
  * @param receiver list of references to receiving users.
  */
case class Mail(user: String, subject: String, body: String, receiver: List[String])

/**
  * Represents all meta information about sent e-mails. It's important to note, that we cannot use the users e-mail
  * address, because we have no access to all e-mail providers SMTP server. So, we have to use only Viva con Agua e-mail
  * addresses. Default should be a no-reply address of Viva con Agua.
  *
  * @author Johann Sell
  * @param sendingAddress the e-mail address used to send the e-mail.
  * @param created date of creation of the e-mail.
  * @param sent date of the sending the e-mail.
  */
case class MailMeta(sendingAddress: String, created: Long, sent: Option[Long])