/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * * Neither the name of Quorum Born IT nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 * * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade
 * de Lisboa (Departamento de Informática), hereby referred to as the Beneficiary, is the
 * sole demonstrated end-user and ultimately the only beneficiary of the redistributed binary
 * form and/or source code.
 * * The Beneficiary is entrusted with either the binary form, the source code, or both, and
 * by accepting it, accepts the terms of this License.
 * * Redistribution of any binary form and/or source code is only allowed in the scope of the
 * Universidade de Lisboa FenixEdu(™)’s implementation projects.
 * * This license and conditions of redistribution of source code/binary can only be reviewed
 * by the Steering Comittee of FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.domain.forwardpayments.implementations.onlinepaymentsgateway.sibs;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.onlinepaymentsgateway.api.CheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.onlinepaymentsgateway.sibs.sdk.SibsResultCodeType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentImplementation;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SibsOnlinePaymentsGatewayForwardImplementation implements IForwardPaymentImplementation {

    private static final String ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID =
            "error.SibsOnlinePaymentsGatewayForwardImplementation.paymentStatus.unexpected.number.transactions.by.merchantTransactionId";
    public static final String ONLINE_PAYMENTS_GATEWAY = "ONLINE-PAYMENTS-GATEWAY";

    @Override
    public IForwardPaymentController getForwardPaymentController(final ForwardPayment forwardPayment) {
        //return IForwardPaymentController.getForwardPaymentController(forwardPayment);
        return null;
    }

    @Override
    public String getPaymentURL(ForwardPayment forwardPayment) {
        return forwardPayment.getForwardPaymentConfiguration().getPaymentURL() + "/paymentWidgets.js?checkoutId="
                + forwardPayment.getSibsCheckoutId();
    }

    @Override
    public String getFormattedAmount(ForwardPayment forwardPayment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLogosJspPage(final ForwardPaymentConfiguration forwardPaymentConfiguration) {
        return forwardPaymentConfiguration.getLogosJspPageFile();
    }

    @Override
    public String getWarningBeforeRedirectionJspPage() {
        // TODO Auto-generated method stub
        return null;
    }

    public static final String CONTROLLER_URL = "/treasury/document/forwardpayments/sibsonlinepaymentsgateway";
    private static final String RETURN_FORWARD_PAYMENT_URI = "/returnforwardpayment";
    public static final String RETURN_FORWARD_PAYMENT_URL = CONTROLLER_URL + RETURN_FORWARD_PAYMENT_URI;

    public String getReturnURL(final ForwardPayment forwardPayment) {
        return String.format("%s%s/%s", forwardPayment.getForwardPaymentConfiguration().getReturnURL(),
                RETURN_FORWARD_PAYMENT_URL, forwardPayment.getExternalId());
    }

    @Atomic(mode = TxMode.READ)
    public ForwardPaymentStatusBean prepareCheckout(final ForwardPayment forwardPayment,
            final SibsBillingAddressBean addressBean) {
        final SibsOnlinePaymentsGateway gateway = forwardPayment.getForwardPaymentConfiguration().getSibsOnlinePaymentsGateway();

        final String merchantTransactionId = gateway.generateNewMerchantTransactionId();

        FenixFramework.atomic(() -> {
            if (!StringUtils.isEmpty(forwardPayment.getSibsMerchantTransactionId())) {
                throw new AcademicTreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.sibsMerchantTransactionId.already.filled");
            }

            forwardPayment.setSibsMerchantTransactionId(merchantTransactionId);
        });

        try {
            final CheckoutResultBean checkoutBean = gateway.prepareCheckout(forwardPayment.getDebtAccount(),
                    merchantTransactionId, forwardPayment.getAmount(), getReturnURL(forwardPayment), addressBean);

            final ForwardPaymentStateType type = translateForwardPaymentStateType(checkoutBean.getOperationResultType(), false);
            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(checkoutBean.isOperationSuccess(), type,
                    checkoutBean.getPaymentGatewayResultCode(), checkoutBean.getPaymentGatewayResultDescription(),
                    checkoutBean.getRequestLog(), checkoutBean.getResponseLog());

            FenixFramework.atomic(() -> {
                forwardPayment.setSibsCheckoutId(checkoutBean.getCheckoutId());
            });

            FenixFramework.atomic(() -> {
                if (!result.isInvocationSuccess() || (result.getStateType() == ForwardPaymentStateType.REJECTED)) {
                    forwardPayment.reject(checkoutBean.getPaymentGatewayResultCode(),
                            checkoutBean.getPaymentGatewayResultDescription(), checkoutBean.getRequestLog(),
                            checkoutBean.getResponseLog());
                } else {
                    forwardPayment.advanceToRequestState(checkoutBean.getPaymentGatewayResultCode(),
                            checkoutBean.getPaymentGatewayResultDescription(), checkoutBean.getRequestLog(),
                            checkoutBean.getResponseLog());
                }
            });

            result.defineSibsOnlinePaymentBrands(checkoutBean.getPaymentBrands());

            return result;

        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                forwardPayment.logException(e, requestBody, responseBody);
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }

    }

    public ForwardPaymentStatusBean paymentStatusByCheckoutId(final ForwardPayment forwardPayment) {
        final SibsOnlinePaymentsGateway gateway = forwardPayment.getForwardPaymentConfiguration().getSibsOnlinePaymentsGateway();

        try {
            PaymentStateBean paymentStateBean = gateway.getPaymentStatusBySibsCheckoutId(forwardPayment.getSibsCheckoutId());

            final String requestLog = paymentStateBean.getRequestLog();
            final String responseLog = paymentStateBean.getResponseLog();

            final ForwardPaymentStateType type =
                    translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

            final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                    paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                    requestLog, responseLog);

            bean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                    paymentStateBean.getAmount());

            return bean;
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                forwardPayment.logException(e, requestBody, responseBody);
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(final ForwardPayment forwardPayment) {
        final SibsOnlinePaymentsGateway gateway = forwardPayment.getForwardPaymentConfiguration().getSibsOnlinePaymentsGateway();

        try {
            PaymentStateBean paymentStateBean = null;
            if (!StringUtils.isEmpty(forwardPayment.getSibsTransactionId())) {
                paymentStateBean = gateway.getPaymentStatusBySibsTransactionId(forwardPayment.getSibsTransactionId());
            } else {
                List<PaymentStateBean> paymentStateBeanList =
                        gateway.getPaymentTransactionsReportListByMerchantId(forwardPayment.getSibsMerchantTransactionId());
                if (paymentStateBeanList.size() != 1) {
                    throw new TreasuryDomainException(ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID);
                }

                paymentStateBean = paymentStateBeanList.get(0);
            }

            final String requestLog = paymentStateBean.getRequestLog();
            final String responseLog = paymentStateBean.getResponseLog();

            final ForwardPaymentStateType type =
                    translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

            final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                    paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                    requestLog, responseLog);

            bean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                    paymentStateBean.getAmount());

            return bean;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                if (!ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID.equals(e.getMessage())) {
                    forwardPayment.logException(e, requestBody, responseBody);
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    private ForwardPaymentStateType translateForwardPaymentStateType(final SibsResultCodeType operationResultType,
            final boolean paid) {

        if (operationResultType == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayForwardImplementation.unknown.payment.state");
        }

        if (paid) {
            if (operationResultType != SibsResultCodeType.SUCCESSFUL_TRANSACTION
                    && operationResultType != SibsResultCodeType.SUCESSFUL_PROCESSED_TRANSACTION_FOR_REVIEW) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.payment.appears.paid.but.inconsistent.with.result.code");
            }

            return ForwardPaymentStateType.PAYED;
        } else if (operationResultType == SibsResultCodeType.PENDING_TRANSACTION) {
            return ForwardPaymentStateType.REQUESTED;
        }

        return ForwardPaymentStateType.REJECTED;
    }

    @Override
    public PostProcessPaymentStatusBean postProcessPayment(final ForwardPayment forwardPayment, final String justification,
            final Optional<String> specificTransactionId) {
        if (!specificTransactionId.isPresent()) {
            throw new TreasuryDomainException(
                    "error.SibsOnlinePaymentsGatewayForwardImplementation.postProcessPayment.specificTransactionId.required");
        }

        final String transactionId = specificTransactionId.get();

        final ForwardPaymentStateType previousState = forwardPayment.getCurrentState();

        final List<ForwardPaymentStatusBean> paymentStatusBeanList = verifyPaymentStatus(forwardPayment);
        final Optional<ForwardPaymentStatusBean> optionalPaymentStatusBean =
                paymentStatusBeanList.stream().filter(bean -> transactionId.equals(bean.getTransactionId())).findFirst();

        if (optionalPaymentStatusBean.isPresent()) {
            if (StringUtils.isEmpty(forwardPayment.getTransactionId()) && paymentStatusBeanList.size() == 1) {
                FenixFramework.atomic(() -> {
                    forwardPayment.setSibsTransactionId(optionalPaymentStatusBean.get().getTransactionId());
                });
            }

            final ForwardPaymentStatusBean paymentStatusBean = optionalPaymentStatusBean.get();

            if (!forwardPayment.getCurrentState().isInStateToPostProcessPayment()) {
                throw new TreasuryDomainException("error.ManageForwardPayments.forwardPayment.not.created.nor.requested",
                        String.valueOf(forwardPayment.getOrderNumber()));
            }

            if (Strings.isNullOrEmpty(justification)) {
                throw new TreasuryDomainException("label.ManageForwardPayments.postProcessPayment.justification.required");
            }

            if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                    .contains(paymentStatusBean.getStateType())) {
                // Do nothing
                return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, false);
            }

            final boolean success = paymentStatusBean.isInPayedState();

            if (!paymentStatusBean.isInvocationSuccess()) {
                throw new TreasuryDomainException("error.ManageForwardPayments.postProcessPayment.invocation.unsucessful",
                        String.valueOf(forwardPayment.getOrderNumber()));
            }

            if (success) {
                FenixFramework.atomic(() -> {
                    forwardPayment.advanceToPayedState(paymentStatusBean.getStatusCode(), paymentStatusBean.getStatusMessage(),
                            paymentStatusBean.getPayedAmount(), paymentStatusBean.getTransactionDate(),
                            paymentStatusBean.getTransactionId(), paymentStatusBean.getAuthorizationNumber(),
                            paymentStatusBean.getRequestBody(), paymentStatusBean.getResponseBody(), justification);
                });

            } else {
                FenixFramework.atomic(() -> {
                    forwardPayment.reject(paymentStatusBean.getStatusCode(), paymentStatusBean.getStatusMessage(),
                            paymentStatusBean.getRequestBody(), paymentStatusBean.getResponseBody());
                });
            }

            return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, true);
        }

        return null;
    }

    @Override
    public String getImplementationCode() {
        return ONLINE_PAYMENTS_GATEWAY;
    }

    @Override
    public List<ForwardPaymentStatusBean> verifyPaymentStatus(ForwardPayment forwardPayment) {
        final SibsOnlinePaymentsGateway gateway = forwardPayment.getForwardPaymentConfiguration().getSibsOnlinePaymentsGateway();

        try {

            final String sibsMerchantTransactionId = forwardPayment.getSibsMerchantTransactionId();
            final List<PaymentStateBean> paymentStateBeanList =
                    gateway.getPaymentTransactionsReportListByMerchantId(sibsMerchantTransactionId);

            final List<ForwardPaymentStatusBean> result = Lists.newArrayList();
            for (PaymentStateBean paymentStateBean : paymentStateBeanList) {
                final String requestLog = paymentStateBean.getRequestLog();
                final String responseLog = paymentStateBean.getResponseLog();

                final ForwardPaymentStateType type =
                        translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

                final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                        paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                        requestLog, responseLog);

                bean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                        paymentStateBean.getAmount());

                result.add(bean);
            }

            return result;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                forwardPayment.logException(e, requestBody, responseBody);
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }

    }
}
