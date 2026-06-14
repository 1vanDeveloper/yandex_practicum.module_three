package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should process cash withdrawal"
    request {
        method 'POST'
        url '/cash?value=50&action=GET'
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 2,
            accountLogin: "test_user",
            transactionType: "WITHDRAW",
            amount: 50.00,
            status: "COMPLETED"
        ])
    }
}
