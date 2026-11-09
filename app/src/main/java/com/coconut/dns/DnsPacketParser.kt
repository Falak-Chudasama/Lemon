package com.coconut.dns

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

data class DnsQuestion(
    val qname: String,
    val qtype: Int,
    val qclass: Int,
    val rawQuestionBytes: ByteArray
)

data class ParsedDnsQuery(
    val txId: Int,
    val flags: Int,
    val question: DnsQuestion,
    val rawDnsPayload: ByteArray
)

data class IpPacketInfo(
    val isIpv6: Boolean,
    val protocol: Int, // 17 = UDP, 6 = TCP
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val tcpSeqNum: Long = 0L,
    val tcpAckNum: Long = 0L,
    val tcpFlags: Int = 0,
    val transportPayloadOffset: Int,
    val transportPayloadLength: Int
)

object DnsPacketParser {

    fun parseIpPacket(packet: ByteArray, length: Int): IpPacketInfo? {
        if (length < 20) return null
        val version = (packet[0].toInt() shr 4) and 0x0F

        if (version == 4) {
            val ihl = (packet[0].toInt() and 0x0F) * 4
            if (length < ihl + 8) return null
            val protocol = packet[9].toInt() and 0xFF
            val srcIp = packet.copyOfRange(12, 16)
            val dstIp = packet.copyOfRange(16, 20)

            if (protocol == 17) { // UDP
                val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
                val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
                val payloadOffset = ihl + 8
                val payloadLen = (udpLen - 8).coerceAtLeast(0)
                if (payloadOffset + payloadLen > length) return null

                return IpPacketInfo(
                    isIpv6 = false,
                    protocol = protocol,
                    srcIp = srcIp,
                    dstIp = dstIp,
                    srcPort = srcPort,
                    dstPort = dstPort,
                    transportPayloadOffset = payloadOffset,
                    transportPayloadLength = payloadLen
                )
            } else if (protocol == 6) { // TCP
                if (length < ihl + 20) return null
                val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
                val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                val seqNum = ((packet[ihl + 4].toLong() and 0xFF) shl 24) or
                        ((packet[ihl + 5].toLong() and 0xFF) shl 16) or
                        ((packet[ihl + 6].toLong() and 0xFF) shl 8) or
                        (packet[ihl + 7].toLong() and 0xFF)
                val ackNum = ((packet[ihl + 8].toLong() and 0xFF) shl 24) or
                        ((packet[ihl + 9].toLong() and 0xFF) shl 16) or
                        ((packet[ihl + 10].toLong() and 0xFF) shl 8) or
                        (packet[ihl + 11].toLong() and 0xFF)
                val dataOffset = ((packet[ihl + 12].toInt() shr 4) and 0x0F) * 4
                val tcpFlags = packet[ihl + 13].toInt() and 0xFF
                val payloadOffset = ihl + dataOffset
                val payloadLen = (length - payloadOffset).coerceAtLeast(0)

                return IpPacketInfo(
                    isIpv6 = false,
                    protocol = protocol,
                    srcIp = srcIp,
                    dstIp = dstIp,
                    srcPort = srcPort,
                    dstPort = dstPort,
                    tcpSeqNum = seqNum,
                    tcpAckNum = ackNum,
                    tcpFlags = tcpFlags,
                    transportPayloadOffset = payloadOffset,
                    transportPayloadLength = payloadLen
                )
            }
        } else if (version == 6) {
            if (length < 40) return null
            val nextHeader = packet[6].toInt() and 0xFF
            val srcIp = packet.copyOfRange(8, 24)
            val dstIp = packet.copyOfRange(24, 40)

            if (nextHeader == 17 && length >= 48) { // UDP
                val srcPort = ((packet[40].toInt() and 0xFF) shl 8) or (packet[41].toInt() and 0xFF)
                val dstPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
                val udpLen = ((packet[44].toInt() and 0xFF) shl 8) or (packet[45].toInt() and 0xFF)
                val payloadOffset = 48
                val payloadLen = (udpLen - 8).coerceAtLeast(0)
                if (payloadOffset + payloadLen > length) return null

                return IpPacketInfo(
                    isIpv6 = true,
                    protocol = nextHeader,
                    srcIp = srcIp,
                    dstIp = dstIp,
                    srcPort = srcPort,
                    dstPort = dstPort,
                    transportPayloadOffset = payloadOffset,
                    transportPayloadLength = payloadLen
                )
            } else if (nextHeader == 6 && length >= 60) { // TCP
                val srcPort = ((packet[40].toInt() and 0xFF) shl 8) or (packet[41].toInt() and 0xFF)
                val dstPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
                val seqNum = ((packet[44].toLong() and 0xFF) shl 24) or
                        ((packet[45].toLong() and 0xFF) shl 16) or
                        ((packet[46].toLong() and 0xFF) shl 8) or
                        (packet[47].toLong() and 0xFF)
                val ackNum = ((packet[48].toLong() and 0xFF) shl 24) or
                        ((packet[49].toLong() and 0xFF) shl 16) or
                        ((packet[50].toLong() and 0xFF) shl 8) or
                        (packet[51].toLong() and 0xFF)
                val dataOffset = ((packet[52].toInt() shr 4) and 0x0F) * 4
                val tcpFlags = packet[53].toInt() and 0xFF
                val payloadOffset = 40 + dataOffset
                val payloadLen = (length - payloadOffset).coerceAtLeast(0)

                return IpPacketInfo(
                    isIpv6 = true,
                    protocol = nextHeader,
                    srcIp = srcIp,
                    dstIp = dstIp,
                    srcPort = srcPort,
                    dstPort = dstPort,
                    tcpSeqNum = seqNum,
                    tcpAckNum = ackNum,
                    tcpFlags = tcpFlags,
                    transportPayloadOffset = payloadOffset,
                    transportPayloadLength = payloadLen
                )
            }
        }
        return null
    }

    fun parseDnsQuery(dnsBytes: ByteArray, offset: Int, length: Int): ParsedDnsQuery? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(dnsBytes, offset, length)
        val txId = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val qdcount = buffer.short.toInt() and 0xFFFF

        if (qdcount < 1) return null
        buffer.short // ancount
        buffer.short // nscount
        buffer.short // arcount

        val qnameStart = buffer.position()
        val qname = parseQname(buffer) ?: return null
        if (buffer.remaining() < 4) return null
        val qtype = buffer.short.toInt() and 0xFFFF
        val qclass = buffer.short.toInt() and 0xFFFF
        val qnameEnd = buffer.position()

        val rawQuestion = dnsBytes.copyOfRange(qnameStart, qnameEnd)
        val rawDnsPayload = dnsBytes.copyOfRange(offset, offset + length)

        return ParsedDnsQuery(
            txId = txId,
            flags = flags,
            question = DnsQuestion(
                qname = qname,
                qtype = qtype,
                qclass = qclass,
                rawQuestionBytes = rawQuestion
            ),
            rawDnsPayload = rawDnsPayload
        )
    }

    private fun parseQname(buffer: ByteBuffer): String? {
        val sb = StringBuilder()
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                if (buffer.hasRemaining()) buffer.get()
                break
            }
            if (buffer.remaining() < len) return null
            val labelBytes = ByteArray(len)
            buffer.get(labelBytes)
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(labelBytes, Charsets.US_ASCII))
        }
        return sb.toString()
    }

    /**
     * Constructs a RFC 1035 compliant NXDOMAIN response (RCODE=3) for a blocked domain.
     */
    fun buildNxDomainResponse(query: ParsedDnsQuery): ByteArray {
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12)
        header.putShort(query.txId.toShort())
        // Flags: QR=1 (response), Opcode=0, AA=0, TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN) -> 0x8183
        header.putShort(0x8183.toShort())
        header.putShort(1.toShort()) // QDCOUNT = 1
        header.putShort(0.toShort()) // ANCOUNT = 0
        header.putShort(0.toShort()) // NSCOUNT = 0
        header.putShort(0.toShort()) // ARCOUNT = 0
        out.write(header.array())
        out.write(query.question.rawQuestionBytes)
        return out.toByteArray()
    }

    /**
     * Constructs a RFC 1035 compliant SERVFAIL response (RCODE=2).
     */
    fun buildServFailResponse(txId: Int, questionBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12)
        header.putShort(txId.toShort())
        header.putShort(0x8182.toShort())
        header.putShort(1.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        out.write(header.array())
        out.write(questionBytes)
        return out.toByteArray()
    }

    /**
     * Wraps a DNS response payload into an IPv4/UDP or IPv6/UDP packet ready to write to the TUN.
     * Computes mandatory RFC 2460/8200 IPv6 UDP checksum using the IPv6 pseudo-header,
     * and IPv4 UDP & IP header checksums.
     */
    fun wrapInIpUdpPacket(
        dnsPayload: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        isIpv6: Boolean
    ): ByteArray {
        val udpLength = 8 + dnsPayload.size

        if (!isIpv6) {
            val totalLength = 20 + udpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv4 Header (20 bytes)
            packet.put(0x45.toByte()) // Version 4, IHL 5
            packet.put(0x00.toByte()) // DSCP/ECN
            packet.putShort(totalLength.toShort())
            packet.putShort(0x0000.toShort()) // Identification
            packet.putShort(0x4000.toShort()) // Flags: Don't Fragment
            packet.put(64.toByte()) // TTL
            packet.put(17.toByte()) // Protocol: UDP
            packet.putShort(0.toShort()) // Checksum placeholder
            packet.put(srcIp)
            packet.put(dstIp)

            // Calculate IPv4 Header Checksum
            val headerChecksum = calculateChecksum(packet.array(), 0, 20)
            packet.putShort(10, headerChecksum.toShort())

            // UDP Header (8 bytes)
            packet.position(20)
            packet.putShort(srcPort.toShort())
            packet.putShort(dstPort.toShort())
            packet.putShort(udpLength.toShort())
            packet.putShort(0.toShort()) // Checksum placeholder

            // DNS Payload
            packet.put(dnsPayload)

            // Calculate IPv4 UDP Checksum using IPv4 Pseudo-Header
            val pseudoHeader = ByteBuffer.allocate(12)
            pseudoHeader.put(srcIp)
            pseudoHeader.put(dstIp)
            pseudoHeader.put(0.toByte()) // Zero
            pseudoHeader.put(17.toByte()) // Protocol UDP
            pseudoHeader.putShort(udpLength.toShort())

            val udpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 12,
                packet.array(), 20, udpLength
            )
            val finalUdpChecksum = if (udpChecksum == 0) 0xFFFF else udpChecksum
            packet.putShort(26, finalUdpChecksum.toShort())

            return packet.array()
        } else {
            val totalLength = 40 + udpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv6 Header (40 bytes)
            packet.putInt(0x60000000) // Version 6, Traffic Class 0, Flow Label 0
            packet.putShort(udpLength.toShort()) // Payload length
            packet.put(17.toByte()) // Next Header: UDP
            packet.put(64.toByte()) // Hop Limit
            packet.put(srcIp)
            packet.put(dstIp)

            // UDP Header (8 bytes)
            packet.putShort(srcPort.toShort())
            packet.putShort(dstPort.toShort())
            packet.putShort(udpLength.toShort())
            packet.putShort(0.toShort()) // Checksum placeholder

            // DNS Payload
            packet.put(dnsPayload)

            // Mandatory IPv6 UDP Checksum using RFC 2460 Pseudo-Header:
            // Source IP (16B) + Dest IP (16B) + Length (4B, 32-bit int) + Zero (3B) + NextHeader (1B = 17)
            val pseudoHeader = ByteBuffer.allocate(40)
            pseudoHeader.put(srcIp)
            pseudoHeader.put(dstIp)
            pseudoHeader.putInt(udpLength)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(17.toByte())

            val ipv6UdpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 40,
                packet.array(), 40, udpLength
            )
            val finalIpv6Checksum = if (ipv6UdpChecksum == 0) 0xFFFF else ipv6UdpChecksum
            packet.putShort(46, finalIpv6Checksum.toShort())

            return packet.array()
        }
    }

    /**
     * Constructs a TCP SYN-ACK packet with MSS option (1460 bytes) in response to a client SYN.
     */
    fun buildTcpSynAckPacket(incoming: IpPacketInfo, serverSeqNum: Long): ByteArray {
        val tcpLength = 24 // 20 bytes standard header + 4 bytes MSS option
        val clientAckNum = incoming.tcpSeqNum + 1

        if (!incoming.isIpv6) {
            val totalLength = 20 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv4 Header
            packet.put(0x45.toByte())
            packet.put(0x00.toByte())
            packet.putShort(totalLength.toShort())
            packet.putShort(0x0000.toShort())
            packet.putShort(0x4000.toShort())
            packet.put(64.toByte())
            packet.put(6.toByte()) // TCP
            packet.putShort(0.toShort())
            packet.put(incoming.dstIp) // Swap src/dst
            packet.put(incoming.srcIp)

            val ipChecksum = calculateChecksum(packet.array(), 0, 20)
            packet.putShort(10, ipChecksum.toShort())

            // TCP Header
            packet.position(20)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((6 shl 12) or 0x12).toShort()) // Data offset 6 (24 bytes), flags SYN|ACK
            packet.putShort(65535.toShort()) // Window size
            packet.putShort(0.toShort()) // Checksum placeholder
            packet.putShort(0.toShort()) // Urgent pointer
            // MSS option: Kind=2, Len=4, Value=1460 (0x05B4)
            packet.put(0x02.toByte())
            packet.put(0x04.toByte())
            packet.putShort(1460.toShort())

            val pseudoHeader = ByteBuffer.allocate(12)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())
            pseudoHeader.putShort(tcpLength.toShort())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 12,
                packet.array(), 20, tcpLength
            )
            packet.putShort(36, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        } else {
            val totalLength = 40 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv6 Header
            packet.putInt(0x60000000)
            packet.putShort(tcpLength.toShort())
            packet.put(6.toByte())
            packet.put(64.toByte())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            // TCP Header
            packet.position(40)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((6 shl 12) or 0x12).toShort())
            packet.putShort(65535.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())
            packet.put(0x02.toByte())
            packet.put(0x04.toByte())
            packet.putShort(1460.toShort())

            val pseudoHeader = ByteBuffer.allocate(40)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.putInt(tcpLength)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 40,
                packet.array(), 40, tcpLength
            )
            packet.putShort(56, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        }
    }

    /**
     * Constructs a TCP packet containing DNS payload data (PSH|ACK or PSH|ACK|FIN).
     */
    fun buildTcpDataPacket(
        incoming: IpPacketInfo,
        serverSeqNum: Long,
        clientAckNum: Long,
        payload: ByteArray,
        fin: Boolean = false
    ): ByteArray {
        val tcpLength = 20 + payload.size
        val flags = if (fin) 0x19 else 0x18 // PSH|ACK|FIN (0x19) or PSH|ACK (0x18)

        if (!incoming.isIpv6) {
            val totalLength = 20 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv4 Header
            packet.put(0x45.toByte())
            packet.put(0x00.toByte())
            packet.putShort(totalLength.toShort())
            packet.putShort(0x0000.toShort())
            packet.putShort(0x4000.toShort())
            packet.put(64.toByte())
            packet.put(6.toByte())
            packet.putShort(0.toShort())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            val ipChecksum = calculateChecksum(packet.array(), 0, 20)
            packet.putShort(10, ipChecksum.toShort())

            // TCP Header
            packet.position(20)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((5 shl 12) or flags).toShort())
            packet.putShort(65535.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())
            packet.put(payload)

            val pseudoHeader = ByteBuffer.allocate(12)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())
            pseudoHeader.putShort(tcpLength.toShort())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 12,
                packet.array(), 20, tcpLength
            )
            packet.putShort(36, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        } else {
            val totalLength = 40 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv6 Header
            packet.putInt(0x60000000)
            packet.putShort(tcpLength.toShort())
            packet.put(6.toByte())
            packet.put(64.toByte())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            // TCP Header
            packet.position(40)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((5 shl 12) or flags).toShort())
            packet.putShort(65535.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())
            packet.put(payload)

            val pseudoHeader = ByteBuffer.allocate(40)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.putInt(tcpLength)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 40,
                packet.array(), 40, tcpLength
            )
            packet.putShort(56, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        }
    }

    /**
     * Constructs a pure TCP ACK packet.
     */
    fun buildTcpAckPacket(
        incoming: IpPacketInfo,
        serverSeqNum: Long,
        clientAckNum: Long
    ): ByteArray {
        val tcpLength = 20

        if (!incoming.isIpv6) {
            val totalLength = 20 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv4 Header
            packet.put(0x45.toByte())
            packet.put(0x00.toByte())
            packet.putShort(totalLength.toShort())
            packet.putShort(0x0000.toShort())
            packet.putShort(0x4000.toShort())
            packet.put(64.toByte())
            packet.put(6.toByte())
            packet.putShort(0.toShort())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            val ipChecksum = calculateChecksum(packet.array(), 0, 20)
            packet.putShort(10, ipChecksum.toShort())

            // TCP Header
            packet.position(20)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((5 shl 12) or 0x10).toShort()) // ACK
            packet.putShort(65535.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())

            val pseudoHeader = ByteBuffer.allocate(12)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())
            pseudoHeader.putShort(tcpLength.toShort())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 12,
                packet.array(), 20, tcpLength
            )
            packet.putShort(36, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        } else {
            val totalLength = 40 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv6 Header
            packet.putInt(0x60000000)
            packet.putShort(tcpLength.toShort())
            packet.put(6.toByte())
            packet.put(64.toByte())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            // TCP Header
            packet.position(40)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(serverSeqNum.toInt())
            packet.putInt(clientAckNum.toInt())
            packet.putShort(((5 shl 12) or 0x10).toShort())
            packet.putShort(65535.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())

            val pseudoHeader = ByteBuffer.allocate(40)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.putInt(tcpLength)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 40,
                packet.array(), 40, tcpLength
            )
            packet.putShort(56, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        }
    }

    /**
     * Constructs a standard TCP RST packet to cleanly reject TCP connections at the virtual DNS IP.
     * When a client resolver attempts a TCP connection to port 53, receiving RST prompts the OS resolver
     * to immediately fall back to UDP port 53 without hanging.
     */
    fun buildTcpRstPacket(incoming: IpPacketInfo): ByteArray {
        val hasAck = (incoming.tcpFlags and 0x10) != 0
        val isSyn = (incoming.tcpFlags and 0x02) != 0

        val rstSeqNum: Long
        val rstAckNum: Long
        val rstFlags: Int

        if (hasAck) {
            rstSeqNum = incoming.tcpAckNum
            rstAckNum = 0L
            rstFlags = 0x04 // RST
        } else {
            rstSeqNum = 0L
            rstAckNum = incoming.tcpSeqNum + (if (isSyn) 1L else incoming.transportPayloadLength.coerceAtLeast(1).toLong())
            rstFlags = 0x14 // RST | ACK
        }

        val tcpLength = 20

        if (!incoming.isIpv6) {
            val totalLength = 20 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv4 Header
            packet.put(0x45.toByte())
            packet.put(0x00.toByte())
            packet.putShort(totalLength.toShort())
            packet.putShort(0x0000.toShort())
            packet.putShort(0x4000.toShort())
            packet.put(64.toByte())
            packet.put(6.toByte()) // Protocol: TCP
            packet.putShort(0.toShort())
            packet.put(incoming.dstIp) // Swap src/dst
            packet.put(incoming.srcIp)

            val ipChecksum = calculateChecksum(packet.array(), 0, 20)
            packet.putShort(10, ipChecksum.toShort())

            // TCP Header
            packet.position(20)
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(rstSeqNum.toInt())
            packet.putInt(rstAckNum.toInt())
            packet.putShort(((5 shl 12) or rstFlags).toShort()) // Data offset 5 (20 bytes), flags
            packet.putShort(0.toShort()) // Window size 0
            packet.putShort(0.toShort()) // Checksum placeholder
            packet.putShort(0.toShort()) // Urgent pointer

            // IPv4 TCP Checksum
            val pseudoHeader = ByteBuffer.allocate(12)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte()) // Protocol TCP
            pseudoHeader.putShort(tcpLength.toShort())

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 12,
                packet.array(), 20, tcpLength
            )
            packet.putShort(36, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        } else {
            val totalLength = 40 + tcpLength
            val packet = ByteBuffer.allocate(totalLength)

            // IPv6 Header
            packet.putInt(0x60000000)
            packet.putShort(tcpLength.toShort())
            packet.put(6.toByte()) // Next Header: TCP
            packet.put(64.toByte())
            packet.put(incoming.dstIp)
            packet.put(incoming.srcIp)

            // TCP Header
            packet.putShort(incoming.dstPort.toShort())
            packet.putShort(incoming.srcPort.toShort())
            packet.putInt(rstSeqNum.toInt())
            packet.putInt(rstAckNum.toInt())
            packet.putShort(((5 shl 12) or rstFlags).toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())
            packet.putShort(0.toShort())

            // IPv6 TCP Checksum
            val pseudoHeader = ByteBuffer.allocate(40)
            pseudoHeader.put(incoming.dstIp)
            pseudoHeader.put(incoming.srcIp)
            pseudoHeader.putInt(tcpLength)
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(0.toByte())
            pseudoHeader.put(6.toByte()) // TCP

            val tcpChecksum = calculateTwoBufferChecksum(
                pseudoHeader.array(), 0, 40,
                packet.array(), 40, tcpLength
            )
            packet.putShort(56, (if (tcpChecksum == 0) 0xFFFF else tcpChecksum).toShort())

            return packet.array()
        }
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = data[i + 1].toInt() and 0xFF
            sum += (b1 shl 8) or b2
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun calculateTwoBufferChecksum(
        buf1: ByteArray, off1: Int, len1: Int,
        buf2: ByteArray, off2: Int, len2: Int
    ): Int {
        var sum = 0
        var i = off1
        while (i < off1 + len1 - 1) {
            val b1 = buf1[i].toInt() and 0xFF
            val b2 = buf1[i + 1].toInt() and 0xFF
            sum += (b1 shl 8) or b2
            i += 2
        }
        var carryByte: Int? = null
        if (i < off1 + len1) {
            carryByte = buf1[i].toInt() and 0xFF
        }

        var j = off2
        if (carryByte != null && len2 > 0) {
            val b2 = buf2[j].toInt() and 0xFF
            sum += (carryByte shl 8) or b2
            j++
        }

        while (j < off2 + len2 - 1) {
            val b1 = buf2[j].toInt() and 0xFF
            val b2 = buf2[j + 1].toInt() and 0xFF
            sum += (b1 shl 8) or b2
            j += 2
        }
        if (j < off2 + len2) {
            sum += (buf2[j].toInt() and 0xFF) shl 8
        }

        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    /**
     * Extracts minimum TTL and any CNAME targets from a DNS response payload.
     */
    fun inspectResponse(response: ByteArray): Pair<Long, List<String>> {
        if (response.size < 12) return Pair(60L, emptyList())
        val buffer = ByteBuffer.wrap(response)
        buffer.short // txid
        buffer.short // flags
        val qdcount = buffer.short.toInt() and 0xFFFF
        val ancount = buffer.short.toInt() and 0xFFFF
        buffer.short // nscount
        buffer.short // arcount

        // Skip Question section
        for (i in 0 until qdcount) {
            skipName(buffer)
            if (buffer.remaining() < 4) return Pair(60L, emptyList())
            buffer.short // qtype
            buffer.short // qclass
        }

        var minTtl = 3600L
        val cnames = mutableListOf<String>()

        for (i in 0 until ancount) {
            if (buffer.remaining() < 10) break
            skipName(buffer)
            val type = buffer.short.toInt() and 0xFFFF
            buffer.short // class
            val ttl = buffer.int.toLong() and 0xFFFFFFFFL
            if (ttl in 1 until minTtl) {
                minTtl = ttl
            }
            val rdLength = buffer.short.toInt() and 0xFFFF
            if (type == 5) { // CNAME
                val cname = parseQname(buffer)
                if (cname != null) cnames.add(cname)
            } else {
                if (buffer.remaining() >= rdLength) {
                    buffer.position(buffer.position() + rdLength)
                }
            }
        }
        return Pair(minTtl.coerceIn(10L, 3600L), cnames)
    }

    private fun skipName(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                if (buffer.hasRemaining()) buffer.get()
                break
            } else {
                if (buffer.remaining() >= len) {
                    buffer.position(buffer.position() + len)
                } else break
            }
        }
    }

    /**
     * Builds a standard A-record DNS query for testing upstream connectivity.
     */
    fun buildTestDnsQuery(domain: String, txId: Int = 0x1234): ByteArray {
        val out = ByteArrayOutputStream()
        out.write((txId shr 8) and 0xFF)
        out.write(txId and 0xFF)
        out.write(0x01) // RD = 1
        out.write(0x00)
        out.write(0x00) // QDCOUNT = 1
        out.write(0x01)
        out.write(0x00) // ANCOUNT = 0
        out.write(0x00)
        out.write(0x00) // NSCOUNT = 0
        out.write(0x00)
        out.write(0x00) // ARCOUNT = 0
        out.write(0x00)

        for (part in domain.split(".")) {
            if (part.isEmpty()) continue
            val bytes = part.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.write(0x00) // QTYPE = 1 (A)
        out.write(0x01)
        out.write(0x00) // QCLASS = 1 (IN)
        out.write(0x01)
        return out.toByteArray()
    }
}
