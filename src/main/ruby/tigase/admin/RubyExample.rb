# Tigase Jabber/XMPP Server
# Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, version 3 of the License.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program. Look for COPYING file in the top folder.
# If not, see http://www.gnu.org/licenses/.
# 
# $Rev: $
# Last modified by $Author: $
# $Date: $

require 'java'

import java.lang.System

include_class 'tigase.server.Command'
include_class 'tigase.server.Packet'


num1 = Command.getFieldValue($packet, "num1")
num2 = Command.getFieldValue($packet, "num2")

if num1.nil? || num2.nil?
   res = Packet.commandResultForm($packet)
   Command.addTextField(res, "Note", "This is JRuby script!")
	 Command.addFieldValue(res, "num1", "")
   Command.addFieldValue(res, "num2", "")
   return res
else
  return num1 + num2
end
