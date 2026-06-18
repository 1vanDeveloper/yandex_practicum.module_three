package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should process cash deposit"
    request {
        method 'POST'
        url '/cash?value=100&action=PUT&login=test_user'
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            accountLogin: "test_user",
            transactionType: "DEPOSIT",
            amount: 100.00,
            status: "COMPLETED"
        ])
    }
}
