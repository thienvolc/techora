package com.techora.payment.application.command;

import java.util.Map;

public record HandleVnPayIpnCommand(Map<String, String> params) {
}
